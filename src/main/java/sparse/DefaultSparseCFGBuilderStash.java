package sparse;

import analysis.data.DFF;
import com.google.common.graph.MutableGraph;
import heros.sparse.SparseCFG;
import heros.sparse.SparseCFGBuilder;
import heros.sparse.SparseCFGQueryStat;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Value is the type of DFF
 * Specialized for Constant Propagation
 */
public class DefaultSparseCFGBuilderStash implements SparseCFGBuilder<Unit, SootMethod, DFF> {

    private final static Logger LOGGER = Logger.getLogger(DefaultSparseCFGBuilderStash.class.getName());

    private boolean enableExceptions;

    private boolean log = false;

    private Set<Unit> stmsToRemove;

    public DefaultSparseCFGBuilderStash(boolean enableExceptions) {
        this.enableExceptions = enableExceptions;
    }

    private SootMethod m;

    @Override
    public SparseCFG<Unit, DFF> buildSparseCFG(SootMethod m, DFF d, SparseCFGQueryStat queryStat) {
        this.m = m;
        DirectedGraph<Unit> rawGraph = new BriefUnitGraph(m.getActiveBody());
        //handle zero
//        if (d.toString().equals("<<zero>>")) {
//            JimpleDefaultSparseCFG cfg = new JimpleDefaultSparseCFG(d, rawGraph, null, rawGraph.size());
//            return cfg;
//        }

        Map<Unit, Unit> jumps = sparsify(d, m, rawGraph);

        JimpleDefaultSparseCFG cfg = new JimpleDefaultSparseCFG(d, rawGraph, jumps, rawGraph.size() - stmsToRemove.size());

        return cfg;
    }



    private Map<Unit, Unit> sparsify(DFF d, SootMethod m, DirectedGraph<Unit> graph) {
        stmsToRemove = new LinkedHashSet<>();
        Iterator<Unit> iter = graph.iterator();
        while (iter.hasNext()) {
            Unit unit = iter.next();
            if (!stmsToRemove.contains(unit) && !shouldKeepStmt(unit, d, m, graph)) {
                stmsToRemove.add(unit);
            }
        }
        Map<Unit, Unit> jumps = new HashMap<>();
        UnitPatchingChain units = m.getActiveBody().getUnits();
        for (Unit unit : stmsToRemove) {
            Unit pred = units.getPredOf(unit);
            Unit succ = units.getSuccOf(unit);
            if (!stmsToRemove.contains(pred)) {
                while (stmsToRemove.contains(succ)) {
                    succ = units.getSuccOf(succ);
                }
                jumps.put(pred, succ);
            }
        }
        return jumps;
    }


    private boolean shouldKeepStmt(Unit unit, DFF d, SootMethod m, DirectedGraph<Unit> graph) {

        if(d.toString().equals("<<zero>>")){
            if(unit instanceof JAssignStmt){
                JAssignStmt assign = (JAssignStmt) unit;
                Value rightOp = assign.getRightOp();
                if(rightOp instanceof IntConstant){
                    return true;
                }
            }
        }

        //keep for case 1: if v (local) appears in a stmt
        if (keepForCase1(unit, d, m)) {
            return true;
        }

        // case 3: if stmt accesses T.f (static field) or is a callsite
        if (keepForCase3(unit, d)) {
            return true;
        }

        if (keepControlFlowStmts(unit, graph)) {
            return true;
        }

        return false;
    }

    /**
     * // case 1: if d (local) appears in a stmt
     *
     * @param unit
     * @param m
     */
    private boolean keepForCase1(Unit unit, DFF d, SootMethod m) {
        if (unit instanceof JAssignStmt) {
            JAssignStmt stmt = (JAssignStmt) unit;
            Value leftOp = stmt.getLeftOp();
            Value rightOp = stmt.getRightOp();
            DFF left = DFF.asDFF(leftOp);
            DFF right;

            // handle Casts (Not mentioned)
            if (rightOp instanceof JCastExpr) {
                JCastExpr cast = (JCastExpr) rightOp;
                right = DFF.asDFF(cast.getOp());
            } else {
                right = DFF.asDFF(rightOp);
            }

            if (d.equals(left) || d.equals(right)) {
                if (rightOp instanceof InvokeExpr) {
                }
                return true;
            } else if (rightOp instanceof BinopExpr) {
                BinopExpr binop = (BinopExpr) rightOp;
                Value lop = binop.getOp1();
                Value rop = binop.getOp2();
                if (DFF.asDFF(lop).equals(d) || DFF.asDFF(rop).equals(d)) {
                    return true;
                }
            } else {
                // field case (Case 2: handled here)
                if (d.getRemainingFields(right) != null) {
                    return true;
                } else if (d.getRemainingFields(left) != null) {
                    return true;
                }
            }
        }
        if (unit instanceof Stmt) {
            Stmt stmt = (Stmt) unit;
            if (stmt.containsInvokeExpr()) {
                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                List<Value> args = invokeExpr.getArgs();
                // v as arg
                for (Value arg : args) {
                    DFF argDFF = DFF.asDFF(arg);
                    if (d.equals(argDFF)) {
                        return true;
                    } else if (d.getRemainingFields(argDFF) != null) {
                        return true;
                    }
                }
                // v as base v.m()
                if (invokeExpr instanceof JVirtualInvokeExpr) {
                    Value base = ((JVirtualInvokeExpr) invokeExpr).getBase();
                    if (d.equals(DFF.asDFF(base))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean keepForCase3(Unit unit, DFF d) {
        if (d.getValue() instanceof StaticFieldRef) {
            if (unit instanceof JAssignStmt) {
                Value left = ((JAssignStmt) unit).getLeftOp();
                Value right = ((JAssignStmt) unit).getRightOp();
                //V.f = x
                if (left instanceof StaticFieldRef) {
                    if (left.equivTo(d.getValue())) {
                        return true;
                    }
                }
                //x=V.f
                if (right instanceof StaticFieldRef) {
                    if (right.equivTo(d.getValue())) {
                        return true;
                    }
                }
            }
            if (unit instanceof Stmt) {
                Stmt stmt = (Stmt) unit;
                if (stmt.containsInvokeExpr()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean keepControlFlowStmts(Unit stmt, DirectedGraph<Unit> graph) {
        if (stmt instanceof JIfStmt || stmt instanceof JNopStmt || stmt instanceof JGotoStmt || stmt instanceof JReturnStmt || stmt instanceof JReturnVoidStmt || stmt instanceof JIdentityStmt) {
            return true;
        }
        // or if stmt has multiple successors
        if (graph.getSuccsOf(stmt).size() > 1) {
            return true;
        }
        return false;
    }


    private void logCFG(Logger logger, MutableGraph<Unit> graph, String cfgType, Body body, DFF d) {
        if (log) {
            logger.info(cfgType + "-" + d.toString() + ":\n" +
                    graph.nodes().stream()
                            .map(Objects::toString)
                            .collect(Collectors.joining(System.lineSeparator())) + "\n" + body.toString());
        }
    }

}
