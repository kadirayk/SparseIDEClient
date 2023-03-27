package analysis.flowfunctions.normal;

import analysis.data.DFF;
import heros.flowfunc.Gen;

import java.util.Set;

public class ConstantFF extends Gen<DFF> {

    private AliasHandler aliasHandler;

    public ConstantFF(DFF genValue, DFF zeroValue, AliasHandler aliasHandler) {
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
