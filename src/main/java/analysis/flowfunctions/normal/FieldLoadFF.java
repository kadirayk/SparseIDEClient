package analysis.flowfunctions.normal;

import analysis.data.DFF;
import heros.FlowFunction;
import soot.Value;
import soot.jimple.internal.JInstanceFieldRef;

import java.util.HashSet;
import java.util.Set;

public class FieldLoadFF implements FlowFunction<DFF> {

    private FieldStoreAliasHandler aliasHandler;
    private JInstanceFieldRef fieldRef;
    private Value lhs;

    public FieldLoadFF(JInstanceFieldRef fieldRef, Value lhs, FieldStoreAliasHandler aliasHandler) {
        this.fieldRef = fieldRef;
        this.lhs = lhs;
        this.aliasHandler = aliasHandler;
    }


    @Override
    public Set<DFF> computeTargets(DFF source) {
        Set<DFF> res = new HashSet<>();
        res.add(source);
        if(DFF.asDFF(fieldRef).equals(source)){
            res.add(DFF.asDFF(lhs));
            aliasHandler.handleAliases(res);
        }
        return res;
    }
}
