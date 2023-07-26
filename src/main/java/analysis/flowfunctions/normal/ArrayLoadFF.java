package analysis.flowfunctions.normal;

import analysis.data.DFF;
import analysis.data.MetaInfo;
import heros.FlowFunction;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JInstanceFieldRef;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ArrayLoadFF implements FlowFunction<DFF, MetaInfo> {

    private DFF zeroValue;
    private AliasHandler aliasHandler;
    private JArrayRef arrayRef;
    private Value lhs;
    private Unit unit;
    private MetaInfo info;

    public ArrayLoadFF(JArrayRef arrayRef, Value lhs, DFF zeroValue, AliasHandler aliasHandler, Unit unit, MetaInfo info) {
        this.arrayRef = arrayRef;
        this.lhs = lhs;
        this.zeroValue = zeroValue;
        this.aliasHandler = aliasHandler;
        this.unit = unit;
        this.info = info;
    }


    @Override
    public Set<DFF> computeTargets(DFF source) {
        if(source.equals(zeroValue)){
            return Collections.singleton(source);
        }
        Set<DFF> res = new HashSet<>();
        res.add(source);
        if(DFF.asDFF(arrayRef).equals(source)){
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
