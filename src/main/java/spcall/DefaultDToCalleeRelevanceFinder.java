package spcall;

import analysis.data.DFF;
import heros.sparse.SparseCFGQueryStat;
import heros.spcall.DToCalleRelevanceFinder;
import soot.SootMethod;

public class DefaultDToCalleeRelevanceFinder implements DToCalleRelevanceFinder<SootMethod, DFF> {


    @Override
    public Boolean findRelevance(SootMethod method, DFF dff, SparseCFGQueryStat sparseCFGQueryStat) {
        return true;
    }

}
