package analysis.flowfunctions.normal;

import analysis.data.DFF;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JArrayRef;

import java.util.Set;

public class AliasHandler {

    protected Unit curr;
    protected SootMethod method;

    public AliasHandler(SootMethod method, Unit curr) {
        this.curr = curr;
        this.method = method;
    }

    void handleAliases(Set<DFF> res){
        // dummy
    }

    SootMethod getMethod(){
        return method;
    }

    Unit getStmt(){
        return curr;
    }
}
