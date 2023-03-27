package analysis.flowfunctions.normal;

import aliasing.SparseAliasManager;
import analysis.data.DFF;
import boomerang.scene.Val;
import boomerang.scene.jimple.JimpleVal;
import boomerang.scene.sparse.SparseCFGCache;
import boomerang.util.AccessPath;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.internal.JInstanceFieldRef;

import java.util.Collections;
import java.util.Set;

public class FieldStoreAliasHandler extends AliasHandler {

    private JInstanceFieldRef fieldRef;

    public FieldStoreAliasHandler(SootMethod method, Unit curr, Value lhs) {
        super(method, curr);
        if(lhs instanceof JInstanceFieldRef){
            this.fieldRef = (JInstanceFieldRef) lhs;
        }
    }


    @Override
    public void handleAliases(Set<DFF> res) {
        if(this.fieldRef!=null) {
            SparseAliasManager aliasManager = SparseAliasManager.getInstance(SparseCFGCache.SparsificationStrategy.NONE, true);
            Set<AccessPath> aliases = aliasManager.getAliases((Stmt) curr, method, fieldRef.getBase());
            for (AccessPath alias : aliases) {
                Val base = alias.getBase();
                if (base instanceof JimpleVal) {
                    JimpleVal jval = (JimpleVal) base;
                    Value delegate = jval.getDelegate();
                    if(!delegate.equals(fieldRef.getBase())){
                        res.add(new DFF(delegate, curr, Collections.singletonList(fieldRef.getField())));
                    }
                }
            }
        }
    }
}
