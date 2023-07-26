package analysis.flowfunctions;


import analysis.data.DFF;
import analysis.data.MetaInfo;
import analysis.flowfunctions.call.CallFF;
import heros.FlowFunction;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

import java.util.ArrayList;
import java.util.List;


public class CPACallFlowFunctionProvider implements FlowFunctionProvider<DFF, MetaInfo>{

    private FlowFunction<DFF, MetaInfo> flowFunction;

    public CPACallFlowFunctionProvider(Unit callStmt, SootMethod dest, DFF zeroValue){
        // we want to pass only the mapped parameters and effectively kill everything else. So no identity.v()
        Stmt s = (Stmt) callStmt;
        InvokeExpr ie = s.getInvokeExpr();
        final List<Value> callArgs = ie.getArgs();
        final List<Local> paramLocals = new ArrayList<>(callArgs.size());
        for (int i = 0; i < dest.getParameterCount(); i++) {
            paramLocals.add(dest.getActiveBody().getParameterLocal(i));
        }
        flowFunction = new CallFF(callArgs, dest, zeroValue, paramLocals, new MetaInfo(callStmt, dest.makeRef()));
    }

    public FlowFunction<DFF, MetaInfo> getFlowFunction(){
        return flowFunction;
    }

}
