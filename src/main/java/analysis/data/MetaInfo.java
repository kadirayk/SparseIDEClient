package analysis.data;

import soot.SootMethodRef;
import soot.Unit;

public class MetaInfo {

    private Unit stmt;
    private SootMethodRef methodRef;

    public MetaInfo(Unit stmt, SootMethodRef methodRef) {
        this.stmt = stmt;
        this.methodRef = methodRef;
    }

    @Override
    public String toString() {
        return methodRef.getSignature() + ":" + stmt.toString();
    }
}
