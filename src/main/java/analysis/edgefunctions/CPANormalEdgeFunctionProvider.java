package analysis.edgefunctions;

import analysis.data.DFF;
import analysis.edgefunctions.normal.IntegerAssign;
import analysis.edgefunctions.normal.IntegerBinop;
import heros.EdgeFunction;
import heros.edgefunc.AllBottom;
import heros.edgefunc.EdgeIdentity;
import soot.Unit;
import soot.Value;
import soot.jimple.BinopExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.IntConstant;

public class CPANormalEdgeFunctionProvider {

    private final static EdgeFunction<Integer> ALL_BOTTOM = new AllBottom<>(Integer.MAX_VALUE);

    private EdgeFunction<Integer> edgeFunction;

    public CPANormalEdgeFunctionProvider(Unit src, DFF srcNode, DFF tgtNode, DFF zeroValue){
        edgeFunction = EdgeIdentity.v();
        if (srcNode == zeroValue && tgtNode == zeroValue) {
            edgeFunction = ALL_BOTTOM;
        } else if (src instanceof DefinitionStmt) {
            DefinitionStmt assignment = (DefinitionStmt) src;
            Value lhs = assignment.getLeftOp();
            Value rhs = assignment.getRightOp();
            if(lhs == tgtNode.getValue()){
                // check if lhs is the tgtNode we are looking at and if rhs is a constant integer
                if (rhs instanceof IntConstant) {
                    IntConstant iconst = (IntConstant) rhs;
                    edgeFunction = new IntegerAssign(iconst.value);
                }
                // check if rhs is a binary expression with known values
                else if (rhs instanceof BinopExpr) {
                    BinopExpr binop = (BinopExpr) rhs;
                    edgeFunction = new IntegerBinop(binop, srcNode);
                }
            }
        }
    }


    public EdgeFunction<Integer> getEdgeFunction(){
        return edgeFunction;
    }
}
