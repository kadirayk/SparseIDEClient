package analysis.flowfunctions;

import heros.FlowFunction;

public interface FlowFunctionProvider<D,X> {
    FlowFunction<D,X> getFlowFunction();
}
