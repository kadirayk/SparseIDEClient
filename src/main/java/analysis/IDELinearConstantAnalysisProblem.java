package analysis;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import analysis.data.DFF;
import heros.*;
import heros.edgefunc.AllBottom;
import heros.edgefunc.AllTop;
import heros.edgefunc.EdgeIdentity;
import heros.flowfunc.Gen;
import heros.flowfunc.Identity;
import heros.flowfunc.KillAll;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import soot.Local;
import soot.NullType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.ide.DefaultJimpleIDETabulationProblem;
import soot.jimple.*;

public class IDELinearConstantAnalysisProblem extends DefaultJimpleIDETabulationProblem<DFF, Integer, InterproceduralCFG<Unit, SootMethod>> {

    protected InterproceduralCFG<Unit, SootMethod> icfg;

    private final static EdgeFunction<Integer> ALL_BOTTOM = new AllBottom<>(Integer.MAX_VALUE);

    protected final static Integer TOP = Integer.MIN_VALUE;

    protected final static Integer BOTTOM = Integer.MAX_VALUE;

    private int executeBinOperation(String op, int lhs, int rhs) {
        int res;
        switch (op.trim()) {
            case "+":
                res = lhs + rhs;
                break;
            case "-":
                res = lhs - rhs;
                break;
            case "*":
                res = lhs * rhs;
                break;
            case "/":
                res = lhs / rhs;
                break;
            case "%":
                res = lhs % rhs;
                break;
            default:
                throw new UnsupportedOperationException("Could not execute unknown operation '" + op + "'!");
        }
        return res;
    }

    public IDELinearConstantAnalysisProblem(InterproceduralCFG<Unit, SootMethod> icfg) {
        super(icfg);
        this.icfg = icfg;
    }

    protected class EdgeFunctionComposer implements EdgeFunction<Integer> {

        private final EdgeFunction<Integer> F;
        private final EdgeFunction<Integer> G;

        public EdgeFunctionComposer(EdgeFunction<Integer> F, EdgeFunction<Integer> G) {
            this.F = F;
            this.G = G;
        }

        @Override
        public Integer computeTarget(Integer source) {
            return F.computeTarget(G.computeTarget(source));
        }

        @Override
        public EdgeFunction<Integer> composeWith(EdgeFunction<Integer> secondFunction) {
            return G.composeWith(F.composeWith(secondFunction));
        }

        @Override
        public EdgeFunction<Integer> meetWith(EdgeFunction<Integer> otherFunction) {
            // FIXME: needs improvement, but is good enough to analyze the current target programs
            if (this == ALL_BOTTOM && otherFunction != ALL_BOTTOM) {
                return otherFunction;
            } else if (this != ALL_BOTTOM && otherFunction == ALL_BOTTOM) {
                return this;
            } else {
                return this;
            }
        }

        @Override
        public boolean equalTo(EdgeFunction<Integer> other) {
            return F.equalTo(other);
        }

    }

    @Override
    protected EdgeFunction<Integer> createAllTopFunction() {
        return new AllTop<>(TOP);
    }

    @Override
    protected MeetLattice<Integer> createMeetLattice() {
        return new MeetLattice<Integer>() {
            @Override
            public Integer topElement() {
                return TOP;
            }

            @Override
            public Integer bottomElement() {
                return BOTTOM;
            }

            @Override
            public Integer meet(Integer left, Integer right) {
                if (left == TOP && right != BOTTOM) {
                    return right;
                } else if (right == TOP && left != BOTTOM) {
                    return left;
                } else {
                    return BOTTOM;
                }
            }
        };
    }

    @Override
    protected FlowFunctions<Unit, DFF, SootMethod> createFlowFunctionsFactory() {
        return new FlowFunctions<Unit, DFF, SootMethod>() {
            @Override
            public FlowFunction<DFF> getNormalFlowFunction(Unit curr, Unit succ) {
                // check if we have definitions of constant integers
                if (curr instanceof DefinitionStmt) {
                    DefinitionStmt assignment = (DefinitionStmt) curr;
                    Value lhs = assignment.getLeftOp();
                    Value rhs = assignment.getRightOp();
                    // check if rhs is a constant integer
                    if (rhs instanceof IntConstant) {
                        IntConstant iconst = (IntConstant) rhs;
                        return new Gen(new DFF(lhs, curr), zeroValue());
                    }
                    // check if rhs is a binary expression with known values
                    if (rhs instanceof BinopExpr) {
                        BinopExpr binop = (BinopExpr) rhs;
                        Value lop = binop.getOp1();
                        Value rop = binop.getOp2();
                        return new FlowFunction<DFF>() {
                            @Override
                            public Set<DFF> computeTargets(DFF source) {
                                Set<DFF> res = new HashSet<>();
                                res.add(source);
                                if (source != zeroValue() && (lop == source.getValue() && rop instanceof IntConstant || rop == source.getValue() && lop instanceof IntConstant)) {
                                    res.add(DFF.asDFF(lhs));
                                }
                                return res;
                            }
                        };
                    }
                }
                return Identity.v();
            }

            @Override
            public FlowFunction<DFF> getCallFlowFunction(Unit callStmt, SootMethod dest) {
                Stmt s = (Stmt) callStmt;
                InvokeExpr ie = s.getInvokeExpr();
                final List<Value> callArgs = ie.getArgs();
                final List<Local> paramLocals = new ArrayList<>(callArgs.size());
                for (int i = 0; i < dest.getParameterCount(); i++) {
                    paramLocals.add(dest.getActiveBody().getParameterLocal(i));
                }
                return new FlowFunction<DFF>() {
                    @Override
                    public Set<DFF> computeTargets(DFF source) {
                        //ignore implicit calls to static initializers
                        if (dest.getName().equals(SootMethod.staticInitializerName) && dest.getParameterCount() == 0) {
                            return Collections.emptySet();
                        }
                        Set<DFF> res = new HashSet<>();
                        for (int i = 0; i < callArgs.size(); i++) {
                            // Special case: check if function is called with integer literals as params
                            if (callArgs.get(i) instanceof IntConstant && source == zeroValue()) {
                                res.add(DFF.asDFF(paramLocals.get(i)));
                            }
                            // Ordinary case: just perform the mapping
                            if (callArgs.get(i) == source) {
                                res.add(DFF.asDFF(paramLocals.get(i)));
                            }
                        }
                        return res;
                    }
                };
            }

            @Override
            public FlowFunction<DFF> getReturnFlowFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
                // handle the case: int i = returnConstant();
                if (exitStmt instanceof ReturnStmt) {
                    ReturnStmt returnStmt = (ReturnStmt) exitStmt;
                    Value op = returnStmt.getOp();
                    if (op instanceof Local) {
                        if (callSite instanceof DefinitionStmt) {
                            DefinitionStmt defnStmt = (DefinitionStmt) callSite;
                            Value leftOp = defnStmt.getLeftOp();
                            if (leftOp instanceof Local) {
                                final Local tgtLocal = (Local) leftOp;
                                final Local retLocal = (Local) op;
                                return new FlowFunction<DFF>() {
                                    @Override
                                    public Set<DFF> computeTargets(DFF source) {
                                        if (source.equals(DFF.asDFF(retLocal))) {
                                            // TODO: test this, it was == check before when it was Local
                                            return Collections.singleton(DFF.asDFF(tgtLocal));
                                        }
                                        return Collections.emptySet();
                                    }
                                };
                            }
                        }
                    }
                }
                return KillAll.v();
            }

            @Override
            public FlowFunction<DFF> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
                return Identity.v();
            }
        };
    }

    @Override
    protected EdgeFunctions<Unit, DFF, SootMethod, Integer> createEdgeFunctionsFactory() {
        return new EdgeFunctions<Unit, DFF, SootMethod, Integer>() {
            @Override
            public EdgeFunction<Integer> getNormalEdgeFunction(Unit src, DFF srcNode, Unit tgt, DFF tgtNode) {
                if (srcNode == zeroValue() && tgtNode == zeroValue()) {
                    return ALL_BOTTOM;
                }
                if (src instanceof DefinitionStmt) {
                    DefinitionStmt assignment = (DefinitionStmt) src;
                    Value lhs = assignment.getLeftOp();
                    Value rhs = assignment.getRightOp();
                    // check if lhs is the tgtNode we are looking at and if rhs is a constant integer
                    if (lhs == tgtNode.getValue() && rhs instanceof IntConstant) {
                        IntConstant iconst = (IntConstant) rhs;
                        return new EdgeFunction<Integer>() {
                            @Override
                            public Integer computeTarget(Integer source) {
                                return iconst.value;
                            }

                            @Override
                            public EdgeFunction<Integer> composeWith(EdgeFunction<Integer> secondFunction) {
                                return new EdgeFunctionComposer(secondFunction, this);
                            }

                            @Override
                            public EdgeFunction<Integer> meetWith(EdgeFunction<Integer> otherFunction) {
                                if (this == ALL_BOTTOM && otherFunction != ALL_BOTTOM) {
                                    return otherFunction;
                                } else if (this != ALL_BOTTOM && otherFunction == ALL_BOTTOM) {
                                    return this;
                                } else {
                                    return this;
                                }
                            }

                            @Override
                            public boolean equalTo(EdgeFunction<Integer> other) {
                                return this == other;
                            }
                        };
                    }
                    // check if rhs is a binary expression with known values
                    if (lhs == tgtNode.getValue() && rhs instanceof BinopExpr) {
                        BinopExpr binop = (BinopExpr) rhs;
                        Value lop = binop.getOp1();
                        Value rop = binop.getOp2();
                        String op = binop.getSymbol();
                        return new EdgeFunction<Integer>() {
                            @Override
                            public Integer computeTarget(Integer source) {
                                if (lop == srcNode.getValue() && rop instanceof IntConstant) {
                                    IntConstant ic = (IntConstant) rop;
                                    return executeBinOperation(op, source, ic.value);
                                } else if (rop == srcNode.getValue() && lop instanceof IntConstant) {
                                    IntConstant ic = (IntConstant) lop;
                                    return executeBinOperation(op, ic.value, source);
                                }
                                throw new IllegalStateException("Only linear constant propagation can be specified!");
                            }

                            @Override
                            public EdgeFunction<Integer> meetWith(EdgeFunction otherFunction) {
                                throw new UnsupportedOperationException("int i = j op const .meetWith()");
//                                return this;
                            }

                            @Override
                            public EdgeFunction composeWith(EdgeFunction secondFunction) {
                                return new EdgeFunctionComposer(secondFunction, this);
                            }

                            @Override
                            public boolean equalTo(EdgeFunction other) {
                                return this == other;
                            }
                        };
                    }
                }
                return EdgeIdentity.v();
            }

            @Override
            public EdgeFunction<Integer> getCallEdgeFunction(Unit callStmt, DFF srcNode, SootMethod destinationMethod, DFF destNode) {
                return EdgeIdentity.v();
            }

            @Override
            public EdgeFunction<Integer> getReturnEdgeFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt, DFF exitNode, Unit returnSite, DFF retNode) {
                return EdgeIdentity.v();
            }

            @Override
            public EdgeFunction<Integer> getCallToReturnEdgeFunction(Unit callStmt, DFF callNode, Unit returnSite, DFF returnSideNode) {
                return EdgeIdentity.v();
            }
        };
    }

    @Override
    protected DFF createZeroValue() {
        return DFF.asDFF(new JimpleLocal("<<zero>>", NullType.v()));
    }

    @Override
    public Map<Unit, Set<DFF>> initialSeeds() {
        for (SootClass c : Scene.v().getApplicationClasses()) {
            for (SootMethod m : c.getMethods()) {
                if (!m.hasActiveBody()) {
                    continue;
                }
                if (m.getName().equals("entryPoint")) {
                    return DefaultSeeds.make(Collections.singleton(m.getActiveBody().getUnits().getFirst()), zeroValue());
                }
            }
        }
        throw new IllegalStateException("scene does not contain 'entryPoint'");
    }

}
