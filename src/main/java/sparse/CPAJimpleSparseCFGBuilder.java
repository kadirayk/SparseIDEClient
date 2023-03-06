package sparse;

import analysis.data.DFF;
import heros.sparse.SparseCFG;
import heros.sparse.SparseCFGBuilder;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.BinopExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.internal.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;

import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Logger;

/**
 * Value is the type of DFF
 * Specialized for Constant Propagation
 */
public class CPAJimpleSparseCFGBuilder implements SparseCFGBuilder<Unit, SootMethod, DFF> {

    private final static Logger LOGGER = Logger.getLogger(CPAJimpleSparseCFGBuilder.class.getName());

    private boolean addedInvoke = false;

    private boolean enableExceptions;

    public CPAJimpleSparseCFGBuilder(boolean enableExceptions) {
        this.enableExceptions = enableExceptions;
    }


    @Override
    public SparseCFG<Unit, DFF> buildSparseCFG(SootMethod m, DFF d) {

        DirectedGraph<Unit> graph = new BriefUnitGraph(m.getActiveBody());
        JimpleSparseCFG cfg = new JimpleSparseCFG(d);

        List<Unit> heads = graph.getHeads();
        if (heads.size() > 1) {
            throw new RuntimeException("Multiple heads");
        }
        Unit head = heads.get(0);
        //handle Source
        if (d.toString().equals("<<zero>>")) {
            buildCompleteCFG(head, graph, cfg);
            return cfg;
        }

        //buildCompleteCFG(head, graph, cfg);
        buildSparseCFG(head, null, graph, cfg, d, m);
        //logInfo(cfg);
        return cfg;
    }

    /**
     * DFS traverse Original Graph and keep all the stmts
     *
     * @param curr
     * @param graph
     * @param cfg
     */
    private void buildCompleteCFG(Unit curr, DirectedGraph<Unit> graph, JimpleSparseCFG cfg) {
        List<Unit> succs = graph.getSuccsOf(curr);
        if (succs == null || succs.isEmpty()) {
            return;
        }
        for (Unit succ : succs) {
            if (cfg.addEdge(curr, succ)) {
                buildCompleteCFG(succ, graph, cfg);
            }
        }
    }

    /**
     * DFS traverse Original Graph and keep only required stmts
     *
     * @param curr
     * @param graph
     * @param cfg
     */
    private void buildSparseCFG(Unit curr, Unit prev, DirectedGraph<Unit> graph, JimpleSparseCFG cfg, DFF d, SootMethod m) {
        List<Unit> succs = graph.getSuccsOf(curr);
        if (succs == null || succs.isEmpty()) {
            return;
        }
        for (Unit succ : succs) {
            if (shouldKeepStmt(succ, d, m, graph)) {
                boolean addedEdge;
                if (prev != null) {
                    addedEdge = cfg.addEdge(prev, succ);
                } else {
                    addedEdge = cfg.addEdge(curr, succ);
                }
                if (addedEdge) {
                    buildSparseCFG(succ, null, graph, cfg, d, m);
                }
            } else {
                if (prev == null) {
                    buildSparseCFG(succ, curr, graph, cfg, d, m);
                } else {
                    buildSparseCFG(succ, prev, graph, cfg, d, m);
                }
            }
        }
    }


    private boolean shouldKeepStmt(Unit unit, DFF d, SootMethod m, DirectedGraph<Unit> graph) {

        //keep the stmt which generates the D
        if (d.getGeneratedAt()!=null && unit.toString().equals(d.getGeneratedAt().toString())) {
            // TODO: would this be a problem?
            return true;
        }

        //keep for case 1: if v (local) appears in a stmt
        if (keepForCase1(unit, d, m)) {
            return true;
        }

        // case 2: if stmt refers to v alone or v.f (instance field) handled in case 1
        //handleCase2(stmt, sparseCFG);

        // case 3: if stmt accesses T.f (static field) or is a callsite
        if (keepForCase3(unit, d)) {
            return true;
        }

        // not mentioned in paper: keep all the stmts whose LHS is an instance field.
        // a field store might affect the current D, but if we remove it from sparse-CFG, we won't capture its effect.
        if (keepFieldStore(unit)) {
            return true;
        }

//        if (keepFieldLoad(unit)) {
//            return true;
//        }

        if (keepControlFlowStmts(unit, graph)) {
            return true;
        }

        if (addedInvoke) {
            addedInvoke = false;
            return true;
        }

        return false;
    }

    //**********

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
                if(rightOp instanceof InvokeExpr){
                    addedInvoke = true;
                }
                return true;
            } else if(rightOp instanceof BinopExpr){
                    BinopExpr binop = (BinopExpr) rightOp;
                    Value lop = binop.getOp1();
                    Value rop = binop.getOp2();
                    if(DFF.asDFF(lop).equals(d) || DFF.asDFF(rop).equals(d)){
                        return true;
                    }
            }else {
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
                        addedInvoke = true;
                        return true;
                    } else if (d.getRemainingFields(argDFF) != null) {
                        addedInvoke = true;
                        return true;
                    }
                }
                // v as base v.m()
                if (invokeExpr instanceof JVirtualInvokeExpr) {
                    Value base = ((JVirtualInvokeExpr) invokeExpr).getBase();
                    if (d.equals(DFF.asDFF(base))) {
                        addedInvoke = true;
                        return true;
                    }
                    // D can be among aliases of base, if so keep v.m() for D
//                    else if (AliasManager.isTargetDFFOrAlias(stmt, m, base, d)) {
//                        addedInvoke = true;
//                        return true;
//                    }
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
        if (stmt instanceof JIfStmt || stmt instanceof JNopStmt || stmt instanceof JGotoStmt || stmt instanceof JReturnStmt || stmt instanceof JReturnVoidStmt) {
            return true;
        }
        if (stmt instanceof JIdentityStmt) {
            JIdentityStmt id = (JIdentityStmt) stmt;
            if (id.getRightOp() instanceof JCaughtExceptionRef) {
                return true;
            }
        }
        // or if stmt has multiple successors
        if(graph.getSuccsOf(stmt).size()>1){
            return true;
        }
        return false;
    }

    private boolean keepFieldStore(Unit unit) {
        if (unit instanceof JAssignStmt) {
            Value left = ((JAssignStmt) unit).getLeftOp();
            //V.f = x
            if (left instanceof JInstanceFieldRef) {
                // TODO: maybe this can be handled similar to aliases of v.m(), but did not work like below
                //JInstanceFieldRef fieldRef = (JInstanceFieldRef) left;
                //Value base = fieldRef.getBase();
                //if(isTargetDFFOrAlias((Stmt) unit, method, base, sparseCFG.getD())){
                return true;
                //}
            }
        }
        return false;
    }

    private boolean keepFieldLoad(Unit unit) {
        if (unit instanceof JAssignStmt) {
            Value left = ((JAssignStmt) unit).getRightOp();
            //V.f = x
            if (left instanceof JInstanceFieldRef) {
                // TODO: maybe this can be handled similar to aliases of v.m(), but did not work like below
                //JInstanceFieldRef fieldRef = (JInstanceFieldRef) left;
                //Value base = fieldRef.getBase();
                //if(isTargetDFFOrAlias((Stmt) unit, method, base, sparseCFG.getD())){
                return true;
                //}
            }
        }
        return false;
    }


    private void logInfo(JimpleSparseCFG sparseCFG) {
        String str = sparseCFG.getGraph().edges().toString();
        LOGGER.info(MessageFormat.format("SparseCFG for {0}: {1}", sparseCFG.getD(), str));
    }

}
