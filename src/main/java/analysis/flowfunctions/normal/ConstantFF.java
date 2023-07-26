package analysis.flowfunctions.normal;

import analysis.data.DFF;
import analysis.data.MetaInfo;
import heros.flowfunc.Gen;
import soot.Unit;

import java.util.Set;

public class ConstantFF extends Gen<DFF, MetaInfo> {

    private AliasHandler aliasHandler;
    private Unit unit;

    public ConstantFF(DFF genValue, DFF zeroValue, AliasHandler aliasHandler, Unit unit, MetaInfo info) {
        super(genValue, zeroValue, info);
        this.aliasHandler = aliasHandler;
        this.unit = unit;
    }

    @Override
    public Set<DFF> computeTargets(DFF source) {
        Set<DFF> res = super.computeTargets(source);
        aliasHandler.handleAliases(res);
        return res;
    }

}
