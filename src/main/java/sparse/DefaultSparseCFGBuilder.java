package sparse;

import analysis.data.DFF;
import com.google.common.collect.Iterators;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import fj.P;
import heros.solver.Pair;
import heros.sparse.SparseCFG;
import heros.sparse.SparseCFGBuilder;
import heros.sparse.SparseCFGQueryStat;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Value is the type of DFF
 * Specialized for Constant Propagation
 */
public class DefaultSparseCFGBuilder implements SparseCFGBuilder<Unit, SootMethod, DFF> {

    private final static Logger LOGGER = Logger.getLogger(CPAJimpleSparseCFGBuilder.class.getName());

    private boolean addedInvoke = false;

    private boolean enableExceptions;

    private boolean log = false;

    private Set<Unit> stmsToRemove;

    public DefaultSparseCFGBuilder(boolean enableExceptions) {
        this.enableExceptions = enableExceptions;
    }

    private SootMethod m;

    @Override
    public SparseCFG<Unit, DFF> buildSparseCFG(SootMethod m, DFF d, SparseCFGQueryStat queryStat) {
        this.m = m;
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


    protected Iterator<Unit> getBFSIterator(MutableGraph<Unit> graph, Unit head) {
        Traverser<Unit> traverser = Traverser.forGraph(graph);
        return traverser.breadthFirst(head).iterator();
    }


    protected MutableGraph<Unit> convertToMutableGraph(DirectedGraph<Unit> rawGraph) {
        MutableGraph<Unit> mGraph = GraphBuilder.directed().build();
        Iterator<Unit> iterator = rawGraph.iterator();
        while (iterator.hasNext()){
            Unit next = iterator.next();
            List<Unit> succsOf = rawGraph.getSuccsOf(next);
            for (Unit unit : succsOf) {
                mGraph.putEdge(next, unit);
            }
        }
        int initialSize = rawGraph.size();
        int finalSize = mGraph.nodes().size();
        if (initialSize != finalSize) {
            System.out.println("Graph size differs after conversion to mutable graph");
        }
        return mGraph;
    }

//    protected MutableGraph<Unit> convertToMutableGraph(DirectedGraph<Unit> rawGraph) {
//        int initialSize = rawGraph.size();
//        MutableGraph<Unit> mGraph = GraphBuilder.directed().build();
//        List<Unit> heads = rawGraph.getHeads();
//        for (Unit head : heads) {
//            addToMutableGraph(rawGraph, head, mGraph);
//        }
//        int finalSize = mGraph.nodes().size();
//        if (initialSize != finalSize) {
//            System.out.println("Graph size differs after conversion to mutable graph");
//        }
//        return mGraph;
//    }

    protected void addToMutableGraph(
            DirectedGraph<Unit> graph, Unit curr, MutableGraph<Unit> mutableGraph) {
        List<Unit> succsOf = graph.getSuccsOf(curr);
        for (Unit succ : succsOf) {
            if (!mutableGraph.hasEdgeConnecting(curr, succ) && !curr.equals(succ)) {
                mutableGraph.putEdge(curr, succ);
                addToMutableGraph(graph, succ, mutableGraph);
            }else{
                if(m.getSignature().equals("<org.apache.xpath.patterns.StepPattern: boolean checkProximityPosition(org.apache.xpath.XPathContext,int,org.apache.xml.dtm.DTM,int,int)>")){
                    System.out.println(curr);
                }
            }
        }
    }


    /**
     * DFS traverse Original Graph and keep all the stmts
     *
     * @param curr
     * @param graph
     * @param cfg
     */
    private void buildCompleteCFG(Unit curr, DirectedGraph<Unit> graph, JimpleSparseCFG cfg) {
        List<Unit> succs = graph.getSuccsOf(curr);
        if (succs == null || succs.isEmpty()) {
            return;
        }
        for (Unit succ : succs) {
            if (cfg.addEdge(curr, succ)) {
                buildCompleteCFG(succ, graph, cfg);
            }
        }
    }

    /**
     * DFS traverse Original Graph and keep only required stmts
     *
     * @param curr
     * @param graph
     * @param cfg
     */
    private void buildSparseCFG(Unit curr, Unit prev, DirectedGraph<Unit> graph, JimpleSparseCFG cfg, DFF d, SootMethod m) {
        List<Unit> succs = graph.getSuccsOf(curr);
        if (succs == null || succs.isEmpty()) {
            return;
        }
        for (Unit succ : succs) {
            if (shouldKeepStmt(succ, d, m, graph)) {
                boolean addedEdge;
                if (prev != null) {
                    addedEdge = cfg.addEdge(prev, succ);
                } else {
                    addedEdge = cfg.addEdge(curr, succ);
                }
                if (addedEdge) {
                    buildSparseCFG(succ, null, graph, cfg, d, m);
                }
            } else {
                if (prev == null) {
                    buildSparseCFG(succ, curr, graph, cfg, d, m);
                } else {
                    buildSparseCFG(succ, prev, graph, cfg, d, m);
                }
            }
        }
    }


    private boolean shouldKeepStmt(Unit unit, DFF d, SootMethod m, DirectedGraph<Unit> graph) {

        //keep the stmt which generates the D
        if (d.getGeneratedAt() != null && unit.toString().equals(d.getGeneratedAt().toString())) {
            return true;
        }

        //keep for case 1: if v (local) appears in a stmt
        if (keepForCase1(unit, d, m)) {
            return true;
        }

        // case 3: if stmt accesses T.f (static field) or is a callsite
        if (keepForCase3(unit, d)) {
            return true;
        }

        // not mentioned in paper: keep all the stmts whose LHS is an instance field.
        // a field store might affect the current D, but if we remove it from sparse-CFG, we won't capture its effect.
        if (keepFieldStore(unit)) {
            return true;
        }

//        if (keepFieldLoad(unit)) {
//            return true;
//        }

        if (keepControlFlowStmts(unit, graph)) {
            return true;
        }

        if (addedInvoke) {
            addedInvoke = false;
            return true;
        }

        return false;
    }

    //**********

    /**
     * // case 1: if d (local) appears in a stmt
     *
     * @param unit
     * @param m
     */
    private boolean keepForCase1(Unit unit, DFF d, SootMethod m) {
        if (unit instanceof JAssignStmt) {
            JAssignStmt stmt = (JAssignStmt) unit;
            Value leftOp = stmt.getLeftOp();
            Value rightOp = stmt.getRightOp();
            DFF left = DFF.asDFF(leftOp);
            DFF right;

            // handle Casts (Not mentioned)
            if (rightOp instanceof JCastExpr) {
                JCastExpr cast = (JCastExpr) rightOp;
                right = DFF.asDFF(cast.getOp());
            } else {
                right = DFF.asDFF(rightOp);
            }

            if (d.equals(left) || d.equals(right)) {
                if (rightOp instanceof InvokeExpr) {
                    addedInvoke = true;
                }
                return true;
            } else if (rightOp instanceof BinopExpr) {
                BinopExpr binop = (BinopExpr) rightOp;
                Value lop = binop.getOp1();
                Value rop = binop.getOp2();
                if (DFF.asDFF(lop).equals(d) || DFF.asDFF(rop).equals(d)) {
                    return true;
                }
            } else {
                // field case (Case 2: handled here)
                if (d.getRemainingFields(right) != null) {
                    return true;
                } else if (d.getRemainingFields(left) != null) {
                    return true;
                }
            }
        }
        if (unit instanceof Stmt) {
            Stmt stmt = (Stmt) unit;
            if (stmt.containsInvokeExpr()) {
                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                List<Value> args = invokeExpr.getArgs();
                // v as arg
                for (Value arg : args) {
                    DFF argDFF = DFF.asDFF(arg);
                    if (d.equals(argDFF)) {
                        addedInvoke = true;
                        return true;
                    } else if (d.getRemainingFields(argDFF) != null) {
                        addedInvoke = true;
                        return true;
                    }
                }
                // v as base v.m()
                if (invokeExpr instanceof JVirtualInvokeExpr) {
                    Value base = ((JVirtualInvokeExpr) invokeExpr).getBase();
                    if (d.equals(DFF.asDFF(base))) {
                        addedInvoke = true;
                        return true;
                    }
                    // D can be among aliases of base, if so keep v.m() for D
//                    else if (AliasManager.isTargetDFFOrAlias(stmt, m, base, d)) {
//                        addedInvoke = true;
//                        return true;
//                    }
                }
            }
        }
        return false;
    }

    private boolean keepForCase3(Unit unit, DFF d) {
        if (d.getValue() instanceof StaticFieldRef) {
            if (unit instanceof JAssignStmt) {
                Value left = ((JAssignStmt) unit).getLeftOp();
                Value right = ((JAssignStmt) unit).getRightOp();
                //V.f = x
                if (left instanceof StaticFieldRef) {
                    if (left.equivTo(d.getValue())) {
                        return true;
                    }
                }
                //x=V.f
                if (right instanceof StaticFieldRef) {
                    if (right.equivTo(d.getValue())) {
                        return true;
                    }
                }
            }
            if (unit instanceof Stmt) {
                Stmt stmt = (Stmt) unit;
                if (stmt.containsInvokeExpr()) {
                    return true;
                }
            }
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

    private boolean keepFieldStore(Unit unit) {
        if (unit instanceof JAssignStmt) {
            Value left = ((JAssignStmt) unit).getLeftOp();
            //V.f = x
            if (left instanceof JInstanceFieldRef) {
                // TODO: maybe this can be handled similar to aliases of v.m(), but did not work like below
                //JInstanceFieldRef fieldRef = (JInstanceFieldRef) left;
                //Value base = fieldRef.getBase();
                //if(isTargetDFFOrAlias((Stmt) unit, method, base, sparseCFG.getD())){
                return true;
                //}
            }
        }
        return false;
    }

    private boolean keepFieldLoad(Unit unit) {
        if (unit instanceof JAssignStmt) {
            Value left = ((JAssignStmt) unit).getRightOp();
            //V.f = x
            if (left instanceof JInstanceFieldRef) {
                // TODO: maybe this can be handled similar to aliases of v.m(), but did not work like below
                //JInstanceFieldRef fieldRef = (JInstanceFieldRef) left;
                //Value base = fieldRef.getBase();
                //if(isTargetDFFOrAlias((Stmt) unit, method, base, sparseCFG.getD())){
                return true;
                //}
            }
        }
        return false;
    }


    private void logCFG(Logger logger, MutableGraph<Unit> graph, String cfgType, Body body, DFF d) {
        if (log) {
            logger.info(cfgType + "-" + d.toString() + ":\n" +
                    graph.nodes().stream()
                            .map(Objects::toString)
                            .collect(Collectors.joining(System.lineSeparator())) + "\n" + body.toString());
        }
    }

}
