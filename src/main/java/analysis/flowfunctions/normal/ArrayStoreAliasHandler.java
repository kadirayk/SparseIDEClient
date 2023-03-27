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
import soot.jimple.internal.JArrayRef;

import java.util.Set;

public class ArrayStoreAliasHandler extends AliasHandler {

    private JArrayRef arrayRef;

    public ArrayStoreAliasHandler(SootMethod method, Unit curr, Value lhs) {
        super(method, curr);
        if (lhs instanceof JArrayRef) {
            this.arrayRef = (JArrayRef) lhs;
        }
    }

    @Override
    public void handleAliases(Set<DFF> res) {
        if (this.arrayRef != null) {
            SparseAliasManager aliasManager = SparseAliasManager.getInstance(SparseCFGCache.SparsificationStrategy.NONE, true);
            Set<AccessPath> aliases = aliasManager.getAliases((Stmt) curr, method, arrayRef.getBase());
            for (AccessPath alias : aliases) {
                Val base = alias.getBase();
                if (base instanceof JimpleVal) {
                    JimpleVal jval = (JimpleVal) base;
                    Value delegate = jval.getDelegate();
                    if(!delegate.equals(arrayRef.getBase())){
                        JArrayRef newRef = new JArrayRef(delegate, arrayRef.getIndex());
                        res.add(new DFF(newRef, curr));
                    }
                }
            }
        }
    }

}
