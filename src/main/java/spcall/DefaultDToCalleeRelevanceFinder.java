package spcall;

import analysis.data.DFF;
import heros.sparse.SparseCFGQueryStat;
import heros.spcall.DToCalleRelevanceFinder;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.Iterator;
import java.util.List;

public class DefaultDToCalleeRelevanceFinder implements DToCalleRelevanceFinder<SootMethod, DFF> {


    @Override
    public Boolean findRelevance(SootMethod method, DFF dff, SparseCFGQueryStat sparseCFGQueryStat) {

        DirectedGraph<Unit> graph = new BriefUnitGraph(method.getActiveBody());

        Iterator<Unit> iter = graph.iterator();
        while (iter.hasNext()){
            Unit unit = iter.next();
            if(isRelevant(unit, dff)){
                return true;
            }
        }
        return false;
    }

    private boolean isRelevant(Unit unit, DFF dff){
        return shouldKeepStmt(unit, dff);
    }

    private boolean shouldKeepStmt(Unit unit, DFF d) {
        if(d.toString().equals("<<zero>>")){ // zero should be mapped to callee's context, and back (this is done with keepControlFlowStmts)
            if(unit instanceof JAssignStmt){
                JAssignStmt assign = (JAssignStmt) unit;
                Value rightOp = assign.getRightOp();
                if(rightOp instanceof IntConstant || rightOp instanceof InvokeExpr){
                    return true;
                }
            }
            if(unit instanceof InvokeStmt){
                return true;
            }
            return false;
        }


        //keep for case 1: if v (local) appears in a stmt
        if (keepForCase1(unit, d)) {
            return true;
        }

        // case 3: if stmt accesses T.f (static field) or is a callsite
        if (keepForCase3(unit, d)) {
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
    private boolean keepForCase1(Unit unit, DFF d) {
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
            return true; // currently keep statics always
//            if (unit instanceof JAssignStmt) {
//                Value left = ((JAssignStmt) unit).getLeftOp();
//                Value right = ((JAssignStmt) unit).getRightOp();
//                //V.f = x
//                if (left instanceof StaticFieldRef) {
//                    if (left.equivTo(d.getValue())) {
//                        return true;
//                    }
//                }
//                //x=V.f
//                if (right instanceof StaticFieldRef) {
//                    if (right.equivTo(d.getValue())) {
//                        return true;
//                    }
//                }
//            }
//            if (unit instanceof Stmt) {
//                Stmt stmt = (Stmt) unit;
//                if (stmt.containsInvokeExpr()) {
//                    return true;
//                }
//            }
        }
        return false;
    }


}
