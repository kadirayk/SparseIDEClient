package analysis.edgefunctions.normal;

import heros.EdgeFunction;

/**
 * Think reverse
 */
public class IntegerTop implements EdgeFunction<Integer> {

    private static final IntegerTop instance = new IntegerTop();

    Integer value = Integer.MIN_VALUE;

    private IntegerTop(){
    }

    public static IntegerTop v(){
        return instance;
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
        return secondFunction;
    }

    @Override
    public EdgeFunction<Integer> meetWith(EdgeFunction<Integer> otherFunction) {
        return otherFunction;
    }

    @Override
    public boolean equalTo(EdgeFunction<Integer> edgeFunction) {
        return false;
    }
}