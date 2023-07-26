package analysis.flowfunctions;


import analysis.data.DFF;
import analysis.data.MetaInfo;
import analysis.flowfunctions.normal.*;
import heros.FlowFunction;
import heros.flowfunc.Identity;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.internal.JArrayRef;


public class CPANormalFlowFunctionProvider implements FlowFunctionProvider<DFF, MetaInfo> {

    private FlowFunction<DFF, MetaInfo> flowFunction;

    public CPANormalFlowFunctionProvider(SootMethod method, Unit curr, DFF zeroValue) {
        MetaInfo metaInfo = new MetaInfo(curr, method.makeRef());
        flowFunction = new Identity<>(metaInfo); // always id as fallback
        if (curr instanceof DefinitionStmt) {
            DefinitionStmt assignment = (DefinitionStmt) curr;
            Value lhs = assignment.getLeftOp();
            Value rhs = assignment.getRightOp();
            // assignment of constant integer
            if (rhs instanceof IntConstant) {
                flowFunction = new ConstantFF(new DFF(lhs, curr), zeroValue, AliasHandlerProvider.get(method, curr, lhs), curr, metaInfo);
            } else if (rhs instanceof BinopExpr) {
                // assignment of binop
                BinopExpr binop = (BinopExpr) rhs;
                flowFunction = new BinopFF(lhs, binop, zeroValue, AliasHandlerProvider.get(method, curr, lhs), curr, metaInfo);
            } else if (rhs instanceof Local) {
                // assignment of local
                Local right = (Local) rhs;
                flowFunction = new LocalFF(right, lhs, zeroValue, AliasHandlerProvider.get(method, curr, lhs), curr, metaInfo);
            } else if (rhs instanceof FieldRef) {
                // assignment of instance field
                FieldRef fieldRef = (FieldRef) rhs;
                flowFunction = new FieldLoadFF(fieldRef, lhs, zeroValue, AliasHandlerProvider.get(method, curr, lhs), curr, metaInfo);
            } else if (rhs instanceof JArrayRef) {
                JArrayRef arrRef = (JArrayRef) rhs;
                flowFunction = new ArrayLoadFF(arrRef, lhs, zeroValue, AliasHandlerProvider.get(method, curr, lhs), curr, metaInfo);
            }
        }
    }

    public FlowFunction<DFF, MetaInfo> getFlowFunction() {
        return flowFunction;
    }

}
