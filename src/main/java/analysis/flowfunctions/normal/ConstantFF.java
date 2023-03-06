package analysis.flowfunctions.normal;

import analysis.data.DFF;
import heros.flowfunc.Gen;

import java.util.Set;

public class ConstantFF extends Gen<DFF> {

    private FieldStoreAliasHandler aliasHandler;

    public ConstantFF(DFF genValue, DFF zeroValue, FieldStoreAliasHandler aliasHandler) {
        super(genValue, zeroValue);
        this.aliasHandler = aliasHandler;
    }

    @Override
    public Set<DFF> computeTargets(DFF source) {
        Set<DFF> res = super.computeTargets(source);
        aliasHandler.handleAliases(res);
        return res;
    }
}
