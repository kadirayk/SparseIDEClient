package sparse;

import analysis.data.DFF;
import heros.sparse.SparseCFG;
import heros.sparse.SparseCFGBuilder;
import heros.sparse.SparseCFGQueryStat;
import soot.SootMethod;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.jimple.internal.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.*;

public class IDSparseCFGBuilder implements SparseCFGBuilder<Unit, SootMethod, DFF> {

    private static IDSparseCFGBuilder INSTANCE;

    private static Map<String, Map<DFF, List<Unit>>> idStmts = new LinkedHashMap<>();

    Set<Unit> stmsToRemove;

    private IDSparseCFGBuilder(){

    }

    public static IDSparseCFGBuilder v(){
        if(INSTANCE==null){
            INSTANCE = new IDSparseCFGBuilder();
        }
        return INSTANCE;
    }

    public static void keepStmt(SootMethod m, Unit u, Set<DFF> DFFs, DFF source) {
        if(INSTANCE==null){
            return;
        }

        if(DFFs.size()==1 && DFFs.contains(source)){ // ID
            return;
        }

        String sig = m.getSignature();
        for (DFF d : DFFs) {
            if(d.toString().equals("<<zero>>")){
                continue;
            }
            if (idStmts.containsKey(sig)) {
                Map<DFF, List<Unit>> dffListMap = idStmts.get(sig);
                if (dffListMap.containsKey(d)) {
                    List<Unit> units = dffListMap.get(d);
                    units.add(u);
                } else {
                    List<Unit> units = new ArrayList<>();
                    units.add(u);
                    dffListMap.put(d, units);
                }
            } else {
                Map<DFF, List<Unit>> dffListMap = new LinkedHashMap<>();
                List<Unit> units = new ArrayList<>();
                units.add(u);
                dffListMap.put(d, units);
                idStmts.put(sig, dffListMap);
            }
        }
    }


    @Override
    public SparseCFG<Unit, DFF> buildSparseCFG(SootMethod m, DFF d, SparseCFGQueryStat sparseCFGQueryStat) {
        //log = m.getSignature().contains("com.google.common.util.concurrent.ExecutionList: void execute()");
        DirectedGraph<Unit> rawGraph = new BriefUnitGraph(m.getActiveBody());
        //handle Source
        if (d.toString().equals("<<zero>>")) {
            //logCFG(LOGGER, mCFG, "original", m.getActiveBody(), d);
            JimpleDefaultSparseCFG cfg = new JimpleDefaultSparseCFG(d, rawGraph, null, rawGraph.size());
//            if(m.getSignature().contains("com.google.common.base.Preconditions: java.lang.String badElementIndex(int,int,java.lang.String)")){
//                System.out.println(cfg.toString());
//            }
            //System.out.println(cfg.toString());
            return cfg;
        }

        Map<Unit, Unit> jumps = sparsify(d, m, rawGraph);
        //logCFG(LOGGER, mCFG, "sparse", m.getActiveBody(), d);
        JimpleDefaultSparseCFG cfg = new JimpleDefaultSparseCFG(d, rawGraph, jumps, rawGraph.size() - stmsToRemove.size());
//        if(m.getSignature().contains("com.google.common.base.Preconditions: java.lang.String badElementIndex(int,int,java.lang.String)")){
//            System.out.println(cfg.toString());
//        }
        //System.out.println(cfg.toString());
        return cfg;
    }


    private Map<Unit, Unit> sparsify(DFF d, SootMethod m, DirectedGraph<Unit> graph) {
        stmsToRemove = new LinkedHashSet<>();
        Iterator<Unit> iter = graph.iterator();
        while (iter.hasNext()){
            Unit unit = iter.next();
            if (!stmsToRemove.contains(unit) && !shouldKeepStmt(unit, d, m, graph)) {
                stmsToRemove.add(unit);
            }
        }
        Map<Unit, Unit> jumps = new HashMap<>();
        UnitPatchingChain units = m.getActiveBody().getUnits();
        for (Unit unit : stmsToRemove) {
            Unit pred = units.getPredOf(unit);
            Unit succ = units.getSuccOf(unit);
            if(!stmsToRemove.contains(pred)){
                while(stmsToRemove.contains(succ)){
                    succ = units.getSuccOf(succ);
                }
                jumps.put(pred, succ);
            }
        }
        return jumps;
    }


    private boolean shouldKeepStmt(Unit unit, DFF d, SootMethod m, DirectedGraph<Unit> graph) {
        List<Unit> stmsToKeep = idStmts.get(m.getSignature()).get(d);
        if(stmsToKeep.contains(unit)){
            return true;
        }
        if (keepControlFlowStmts(unit, graph)) {
            return true;
        }
        return false;
    }

    private boolean keepControlFlowStmts(Unit stmt, DirectedGraph<Unit> graph) {
        if (stmt instanceof JIfStmt || stmt instanceof JNopStmt || stmt instanceof JGotoStmt || stmt instanceof JReturnStmt || stmt instanceof JReturnVoidStmt || stmt instanceof JIdentityStmt) {
            return true;
        }
//        if (stmt instanceof JIdentityStmt) {
//            JIdentityStmt id = (JIdentityStmt) stmt;
//            if (id.getRightOp() instanceof JCaughtExceptionRef) {
//                return true;
//            }
//        }
        // or if stmt has multiple successors
        if (graph.getSuccsOf(stmt).size() > 1) {
            return true;
        }
        return false;
    }

}
