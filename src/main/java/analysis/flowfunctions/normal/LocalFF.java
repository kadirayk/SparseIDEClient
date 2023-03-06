package analysis.flowfunctions.normal;

import analysis.data.DFF;
import heros.FlowFunction;
import soot.Local;
import soot.Value;

import java.util.HashSet;
import java.util.Set;

/**
 * Assignment from a single local
 */
public class LocalFF implements FlowFunction<DFF> {

    private Local right;
    private Value lhs;
    private FieldStoreAliasHandler aliasHandler;

    public LocalFF(Local right, Value lhs, FieldStoreAliasHandler aliasHandler) {
        this.right = right;
        this.lhs = lhs;
        this.aliasHandler = aliasHandler;
    }


    @Override
    public Set<DFF> computeTargets(DFF source) {
        Set<DFF> res = new HashSet<>();
        res.add(source);
        if (DFF.asDFF(right).equals(source)) {
            res.add(DFF.asDFF(lhs));
            aliasHandler.handleAliases(res);
        }
        return res;
    }


}
