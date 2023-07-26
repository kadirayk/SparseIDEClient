package analysis.flowfunctions.normal;

import analysis.data.DFF;
import analysis.data.MetaInfo;
import heros.FlowFunction;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.FieldRef;
import soot.jimple.internal.JArrayRef;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Assignment from a single local
 */
public class LocalFF implements FlowFunction<DFF, MetaInfo> {

    private Local right;
    private Value lhs;
    private DFF zeroValue;
    private AliasHandler aliasHandler;
    private Unit unit;
    private MetaInfo info;

    public LocalFF(Local right, Value lhs, DFF zeroValue, AliasHandler aliasHandler, Unit unit, MetaInfo metaInfo) {
        this.right = right;
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
        if (DFF.asDFF(right).equals(source)) {
            res.add(DFF.asDFF(lhs));
            aliasHandler.handleAliases(res);
        }
        // for arrays
        if(source.getValue() instanceof JArrayRef){
            JArrayRef arrayRef = (JArrayRef) source.getValue();
            if(arrayRef.getBase().equals(right)){
                if(!(lhs instanceof FieldRef)){
                    JArrayRef newRef = new JArrayRef(lhs, arrayRef.getIndex());
                    res.add(DFF.asDFF(newRef));
                    aliasHandler.handleAliases(res);
                }
            }
        }
        return res;
    }

    @Override
    public MetaInfo getMeta() {
        return info;
    }


}
