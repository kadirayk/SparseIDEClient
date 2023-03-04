package analysis.edgefunctions;

import heros.EdgeFunction;
import heros.edgefunc.AllBottom;
import heros.edgefunc.AllTop;
import heros.edgefunc.EdgeIdentity;
import soot.Value;
import soot.jimple.BinopExpr;
import soot.jimple.IntConstant;

public class IntegerAssign implements EdgeFunction<Integer> {

    private Integer value;

    public IntegerAssign(Integer value){
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    @Override
    public Integer computeTarget(Integer integer) {
        return value;
    }

    /**
     * first apply this then second
     * @param secondFunction
     * @return
     */
    @Override
    public EdgeFunction<Integer> composeWith(EdgeFunction<Integer> secondFunction) {
        if(secondFunction instanceof EdgeIdentity){
            return this;
        }else if(secondFunction instanceof IntegerAssign){
            return secondFunction;
        }else if(secondFunction instanceof IntegerBinop){
            // Heros paper advises inplace composition for fast execution
            BinopExpr binop = ((IntegerBinop) secondFunction).getBinop();
            Value lop = binop.getOp1();
            Value rop = binop.getOp2();
            if(lop instanceof IntConstant){
                int val = ((IntConstant) lop).value;
                String op = binop.getSymbol();
                int res = IntegerBinop.executeBinOperation(op, value, val);
                return new IntegerAssign(res);
            }else if(rop instanceof IntConstant){
                int val = ((IntConstant) rop).value;
                String op = binop.getSymbol();
                int res = IntegerBinop.executeBinOperation(op, value, val);
                return new IntegerAssign(res);
            }
            throw new RuntimeException("neither lop nor rop is constant");
        }
        return this;
    }

    @Override
    public EdgeFunction<Integer> meetWith(EdgeFunction<Integer> otherFunction) {
        if(otherFunction instanceof EdgeIdentity){
            return this;
        }else if(otherFunction instanceof IntegerAssign){
            Integer valueFromOtherBranch = ((IntegerAssign) otherFunction).getValue(); // input int isn't used anyway
            Integer valueFromThisBranch = this.getValue();
            if(valueFromOtherBranch==valueFromThisBranch){
                return this;
            }else{
                return new AllBottom<>(Integer.MAX_VALUE);
            }
        }else if(otherFunction instanceof IntegerBinop){
            return new AllBottom<>(Integer.MAX_VALUE);
        }else if(otherFunction instanceof AllBottom){
            return otherFunction;
        }
        throw new RuntimeException("can't meeet: " + this.toString() + " and " + otherFunction.toString());
    }

    @Override
    public boolean equalTo(EdgeFunction<Integer> edgeFunction) {
        return false;
    }
}