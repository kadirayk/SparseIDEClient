package analysis.flowfunctions;


import analysis.data.DFF;
import analysis.flowfunctions.normal.*;
import heros.FlowFunction;
import heros.flowfunc.Identity;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.BinopExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.IntConstant;
import soot.jimple.internal.JInstanceFieldRef;


public class CPANormalFlowFunctionProvider implements FlowFunctionProvider<DFF> {

    private FlowFunction<DFF> flowFunction;

    public CPANormalFlowFunctionProvider(SootMethod method, Unit curr, DFF zeroValue) {
        flowFunction = Identity.v(); // always id as fallback
        if (curr instanceof DefinitionStmt) {
            DefinitionStmt assignment = (DefinitionStmt) curr;
            Value lhs = assignment.getLeftOp();
            Value rhs = assignment.getRightOp();
            // assignment of constant integer
            if (rhs instanceof IntConstant) {
                flowFunction = new ConstantFF(new DFF(lhs, curr), zeroValue, new FieldStoreAliasHandler(method, curr, lhs));
            } else if (rhs instanceof BinopExpr) {
                // assignment of binop
                BinopExpr binop = (BinopExpr) rhs;
                flowFunction = new BinopFF(lhs, binop, zeroValue, new FieldStoreAliasHandler(method, curr, lhs));
            } else if (rhs instanceof Local) {
                // assignment of local
                Local right = (Local) rhs;
                flowFunction = new LocalFF(right, lhs, new FieldStoreAliasHandler(method, curr, lhs));
            } else if (rhs instanceof JInstanceFieldRef) {
                // assignment of instance field
                JInstanceFieldRef fieldRef = (JInstanceFieldRef) rhs;
                flowFunction = new FieldLoadFF(fieldRef, lhs, new FieldStoreAliasHandler(method, curr, lhs));
            }
        }
    }

    public FlowFunction<DFF> getFlowFunction() {
        return flowFunction;
    }

}
