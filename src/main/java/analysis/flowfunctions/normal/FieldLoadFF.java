package analysis.flowfunctions.normal;

import analysis.data.DFF;
import analysis.data.MetaInfo;
import heros.FlowFunction;
import soot.Unit;
import soot.Value;
import soot.jimple.FieldRef;
import soot.jimple.internal.JInstanceFieldRef;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FieldLoadFF implements FlowFunction<DFF, MetaInfo> {

    private AliasHandler aliasHandler;
    private FieldRef fieldRef;
    private Value lhs;
    private DFF zeroValue;
    private Unit unit;
    private MetaInfo info;

    public FieldLoadFF(FieldRef fieldRef, Value lhs, DFF zeroValue, AliasHandler aliasHandler, Unit unit, MetaInfo metaInfo) {
        this.fieldRef = fieldRef;
        this.lhs = lhs;
        this.zeroValue = zeroValue;
        this.aliasHandler = aliasHandler;
        this.unit = unit;
        info = metaInfo;
    }


    @Override
    public Set<DFF> computeTargets(DFF source) {
        if(source.equals(zeroValue)){
            return Collections.singleton(source);
        }
        Set<DFF> res = new HashSet<>();
        res.add(source);
        if(DFF.asDFF(fieldRef).equals(source)){
            res.add(DFF.asDFF(lhs));
            aliasHandler.handleAliases(res);
        }
        return res;
    }

    @Override
    public MetaInfo getMeta() {
        return info;
    }
}
