package aliasing;

import analysis.data.DFF;
import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.BoomerangOptions;
import boomerang.DefaultBoomerangOptions;
import boomerang.results.BackwardBoomerangResults;
import boomerang.scene.*;
import boomerang.scene.jimple.*;
import boomerang.scene.sparse.SparseCFGCache;
import boomerang.util.AccessPath;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.Stmt;
import util.SherosStaticFieldRef;
import wpds.impl.Weight;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class SparseAliasManager {

    private static Logger log = LoggerFactory.getLogger(SparseAliasManager.class);

    private static SparseAliasManager INSTANCE;

    private LoadingCache<BackwardQuery, Set<AccessPath>> queryCache;

    private Boomerang boomerangSolver;

    private SootCallGraph sootCallGraph;
    private DataFlowScope dataFlowScope;

    private boolean disableAliasing = false;
    private SparseCFGCache.SparsificationStrategy sparsificationStrategy;
    private boolean ignoreAfterQuery;


    static class BoomerangOptions extends DefaultBoomerangOptions {

        private SparseCFGCache.SparsificationStrategy sparsificationStrategy;
        private boolean ignoreAfterQuery;

        public BoomerangOptions(SparseCFGCache.SparsificationStrategy sparsificationStrategy, boolean ignoreAfterQuery){
            this.sparsificationStrategy = sparsificationStrategy;
            this.ignoreAfterQuery = ignoreAfterQuery;
        }

        @Override
        public SparseCFGCache.SparsificationStrategy getSparsificationStrategy() {
            if(this.sparsificationStrategy==null){
                return SparseCFGCache.SparsificationStrategy.NONE;
            }
            return this.sparsificationStrategy;
        }

        @Override
        public boolean ignoreSparsificationAfterQuery() {
            return this.ignoreAfterQuery;
        }

        @Override
        public int analysisTimeoutMS() {
            return 1000;
        }

        @Override
        public boolean onTheFlyCallGraph() {
            return false;
        }

        @Override
        public StaticFieldStrategy getStaticFieldStrategy() {
            return StaticFieldStrategy.SINGLETON;
        }

        @Override
        public boolean allowMultipleQueries() {
            return true;
        }

        @Override
        public boolean throwFlows() {
            return true;
        }

        @Override
        public boolean trackAnySubclassOfThrowable() {
            return true;
        }
    }

    private static Duration totalAliasingDuration;

    private SparseAliasManager(SparseCFGCache.SparsificationStrategy sparsificationStrategy, boolean ignoreAfterQuery) {
        this.sparsificationStrategy = sparsificationStrategy;
        this.ignoreAfterQuery = ignoreAfterQuery;
        totalAliasingDuration = Duration.ZERO;
        sootCallGraph = new SootCallGraph();
        dataFlowScope = SootDataFlowScope.make(Scene.v());
        setupQueryCache();
    }

    public static Duration getTotalDuration() {
        return totalAliasingDuration;
    }

    public static synchronized SparseAliasManager getInstance(SparseCFGCache.SparsificationStrategy sparsificationStrategy, boolean ignoreAfterQuery) {
        if (INSTANCE == null || INSTANCE.sparsificationStrategy!=sparsificationStrategy || INSTANCE.ignoreAfterQuery!=ignoreAfterQuery) {
            INSTANCE = new SparseAliasManager(sparsificationStrategy, ignoreAfterQuery);
        }
        return INSTANCE;
    }

    private void setupQueryCache() {
        queryCache =
                CacheBuilder.newBuilder()
                        .build(
                                new CacheLoader<BackwardQuery, Set<AccessPath>>() {
                                    @Override
                                    public Set<AccessPath> load(BackwardQuery query) throws Exception {
                                        Set<AccessPath> aliases = queryCache.getIfPresent(query);
                                        if (aliases == null) {
                                            // TODO: stabilize null pointer exception that happens sometimes in boomerang
                                            boomerangSolver =
                                                    new Boomerang(
                                                            sootCallGraph, dataFlowScope, new BoomerangOptions(INSTANCE.sparsificationStrategy, INSTANCE.ignoreAfterQuery));
                                            BackwardBoomerangResults<Weight.NoWeight> results = boomerangSolver.solve(query);
                                            aliases = results.getAllAliases();
                                            boolean debug = false;
                                            if (debug) {
                                                System.out.println(query);
                                                System.out.println("alloc:" + results.getAllocationSites());
                                                System.out.println("aliases:" + aliases);
                                            }//boomerangSolver.unregisterAllListeners();
                                            //boomerangSolver.unregisterAllListeners();
                                            queryCache.put(query, aliases);
                                        }
                                        return aliases;
                                    }
                                });
    }


    /**
     * @param stmt   Statement that contains the value. E.g. Value can be the leftOp
     * @param method Method that contains the Stmt
     * @param value  We actually want to find this local's aliases
     * @return
     */
    public synchronized Set<AccessPath> getAliases(Stmt stmt, SootMethod method, Value value) {
        //log.info(method.getActiveBody().toString());
        //log.info("getAliases call for: " + stmt + " in " + method);
        if (disableAliasing) {
            return Collections.emptySet();
        }
        Stopwatch stopwatch = Stopwatch.createStarted();
        BackwardQuery query = createQuery(stmt, method, value);
        Set<AccessPath> aliases = getAliases(query);
        Duration elapsed = stopwatch.elapsed();
        totalAliasingDuration = totalAliasingDuration.plus(elapsed);
        return aliases;
    }


//    public synchronized Set<AccessPath> getAliases(Stmt stmt, SootMethod method, Value value, Set<AccessPath> allAliases, JimpleField field) {
//        if (allAliases == null) {
//            allAliases = new HashSet<>();
//        }
//        log.info("getAliases call for: " + stmt + " in " + method);
//        if (disableAliasing) {
//            return Collections.emptySet();
//        }
//        Stopwatch stopwatch = Stopwatch.createStarted();
//        BackwardQuery query = createQuery(stmt, method, value);
//        Set<AccessPath> aliases = getAliases(query);
//        for (AccessPath alias : aliases) {
//            if(field!=null && !alias.getFields().contains(field)){
//                alias.getFields().add(field);
//            }
//            if (!allAliases.contains(alias)) {
//                Val baseVal = alias.getBase();
//                if (baseVal instanceof JimpleVal) {
//                    Local aliasBase = (Local) ((JimpleVal) baseVal).getDelegate();
//                    //((JimpleField) ((ArrayList) alias.fieldChain).get(0)).delegate
//                    if (!aliasBase.equals(value)) {
//
//                        allAliases.add(alias);
//                        JimpleField currentField = null;
//                        if(alias.getFields().size()>0){
//                            //TODO: what about deeper fields
//                            currentField = (JimpleField) alias.getFields().stream().findFirst().get();
//                        }
//                        getAliases(stmt, method, aliasBase, allAliases, currentField);
//                    }
//                }
//            }
//        }
//
//        Duration elapsed = stopwatch.elapsed();
//        totalAliasingDuration = totalAliasingDuration.plus(elapsed);
//        log.info("getAliases took: " + elapsed.getSeconds() + " - " + elapsed.getNano());
//
//        return aliases;
//    }


    private BackwardQuery createQuery(Stmt stmt, SootMethod method, Value value) {
            JimpleMethod jimpleMethod = JimpleMethod.of(method);
            Statement statement = JimpleStatement.create(stmt, jimpleMethod);
            JimpleVal val = new JimpleVal(value, jimpleMethod);
            Optional<Statement> first = statement.getMethod().getControlFlowGraph().getSuccsOf(statement).stream().findFirst();
            if(first.isPresent()){
                return BackwardQuery.make(new ControlFlowGraph.Edge(statement, first.get()), val);
            }
            throw new RuntimeException("No successors for: " + statement);
    }

    private Set<AccessPath> getAliases(BackwardQuery query) {
        try {
            return queryCache.get(query);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return Collections.emptySet();
    }

    public static boolean isTargetDFFOrAlias(Stmt stmt, SootMethod method, Value base, DFF target) {
        if (target.toString().equals("<<zero>>")) {
            return false;
        }
        Set<AccessPath> aliases = SparseAliasManager.getInstance(SparseCFGCache.SparsificationStrategy.NONE, true).getAliases(stmt, method, base);
        for (AccessPath alias : aliases) {
            Val baseVal = alias.getBase();
            Value aliasBase = null;
            if (baseVal instanceof JimpleVal) {
                aliasBase = ((JimpleVal) baseVal).getDelegate();
            } else if (baseVal instanceof JimpleStaticFieldVal) {
                JimpleStaticFieldVal staticFieldVal = ((JimpleStaticFieldVal) baseVal);
                JimpleField field = (JimpleField) staticFieldVal.field();
                SootField sootField = field.getSootField();
                SootFieldRef sootFieldRef = sootField.makeRef();
                SherosStaticFieldRef staticFieldRef = new SherosStaticFieldRef(sootFieldRef);
                aliasBase = staticFieldRef;
            } else {
                return false;
            }
            Collection<Field> fields = alias.getFields();
            DFF aliasDFF;
            if (!fields.isEmpty()) {
                final List<SootField> accessPathFields = new ArrayList<>();
                for (final Field field : fields) {
                    if (field instanceof JimpleField) {
                        JimpleField jf = (JimpleField) field;
                        accessPathFields.add(jf.getSootField());
                    }
                }
                aliasDFF = new DFF(aliasBase, stmt, accessPathFields);
            } else {
                aliasDFF = new DFF(aliasBase, stmt);
            }
            if (aliasDFF.equals(target)) {
                return true;
            }
        }
        return false;
    }


}
