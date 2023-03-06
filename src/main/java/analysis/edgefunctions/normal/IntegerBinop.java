package analysis.edgefunctions.normal;

import analysis.IDELinearConstantAnalysisProblem;
import analysis.data.DFF;
import analysis.edgefunctions.IntegerAllBottom;
import heros.EdgeFunction;
import heros.edgefunc.EdgeIdentity;
import soot.Value;
import soot.jimple.BinopExpr;
import soot.jimple.IntConstant;

public class IntegerBinop implements EdgeFunction<Integer> {

    private DFF srcNode;
    private Value lop;
    private Value rop;
    private String op;
    private BinopExpr binop;

    public IntegerBinop(BinopExpr binop, DFF srcNode){
        this.binop = binop;
        this.srcNode = srcNode;
        lop = binop.getOp1();
        rop = binop.getOp2();
        op = binop.getSymbol();
    }

    public BinopExpr getBinop() {
        return binop;
    }

    @Override
    public Integer computeTarget(Integer source) {
        if (lop == srcNode.getValue() && rop instanceof IntConstant) {
            IntConstant ic = (IntConstant) rop;
            return executeBinOperation(op, source, ic.value);
        } else if (rop == srcNode.getValue() && lop instanceof IntConstant) {
            IntConstant ic = (IntConstant) lop;
            return executeBinOperation(op, ic.value, source);
        }
        throw new IllegalStateException("Only linear constant propagation can be specified!");
    }

    public static int executeBinOperation(String op, int lhs, int rhs) {
        int res;
        switch (op.trim()) {
            case "+":
                res = lhs + rhs;
                break;
            case "-":
                res = lhs - rhs;
                break;
            case "*":
                res = lhs * rhs;
                break;
            case "/":
                res = lhs / rhs;
                break;
            case "%":
                res = lhs % rhs;
                break;
            default:
                throw new UnsupportedOperationException("Could not execute unknown operation '" + op + "'!");
        }
        return res;
    }

    @Override
    public EdgeFunction<Integer> meetWith(EdgeFunction otherFunction) {
        if(otherFunction instanceof EdgeIdentity){
            return this;
        }else if(otherFunction instanceof IntegerAssign){
            return new IntegerAllBottom(IDELinearConstantAnalysisProblem.BOTTOM);
        }else if(otherFunction instanceof IntegerBinop){
            return new IntegerAllBottom(IDELinearConstantAnalysisProblem.BOTTOM);
        }else if(otherFunction instanceof IntegerAllBottom){
            return otherFunction;
        }
        throw new RuntimeException("can't meeet: " + this.toString() + " and " + otherFunction.toString());
    }

    @Override
    public EdgeFunction composeWith(EdgeFunction secondFunction) {
        if(secondFunction instanceof EdgeIdentity){
            return this;
        }else if(secondFunction instanceof IntegerAssign){
            return secondFunction;
        }
        return this;
    }

    private int getIntVal(BinopExpr binop){
        Value lop = binop.getOp1();
        Value rop = binop.getOp2();
        if(lop instanceof IntConstant){
            return ((IntConstant) lop).value;
        }else{
            return ((IntConstant) rop).value;
        }
    }

    public DFF getSrcNode(){
        return srcNode;
    }

    @Override
    public boolean equalTo(EdgeFunction other) {
        return this == other;
    }
    }