package aliasing;

import analysis.data.DFF;
import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.DefaultBoomerangOptions;
import boomerang.results.BackwardBoomerangResults;
import boomerang.scene.*;
import boomerang.scene.jimple.*;
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

public class AliasManager {

    private static Logger log = LoggerFactory.getLogger(AliasManager.class);

    private static AliasManager INSTANCE;

    private LoadingCache<BackwardQuery, Set<AccessPath>> queryCache;

    private Boomerang boomerangSolver;

    private SootCallGraph sootCallGraph;
    private DataFlowScope dataFlowScope;

    private boolean disableAliasing = false;


    static class BoomerangOptions extends DefaultBoomerangOptions{
        @Override
        public boolean onTheFlyCallGraph() {
            return false;
        }

        @Override
        public StaticFieldStrategy getStaticFieldStrategy() {
            return StaticFieldStrategy.FLOW_SENSITIVE;
        };

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

    private AliasManager() {
        totalAliasingDuration = Duration.ZERO;
        sootCallGraph = new SootCallGraph();
        dataFlowScope = SootDataFlowScope.make(Scene.v());
        setupQueryCache();
    }

    public static Duration getTotalDuration(){
        return totalAliasingDuration;
    }

    public static synchronized AliasManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AliasManager();
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
                                                            sootCallGraph, dataFlowScope, new BoomerangOptions());
                                            BackwardBoomerangResults<Weight.NoWeight> results = boomerangSolver.solve(query);
                                            aliases = results.getAllAliases();
                                            boolean debug = false;
                                            if(debug) {
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
        log.info("getAliases call for: " + stmt + " in " + method);
        if(disableAliasing){
            return Collections.emptySet();
        }
        Stopwatch stopwatch = Stopwatch.createStarted();
        BackwardQuery query = createQuery(stmt, method, value);
        Set<AccessPath> aliases = getAliases(query);
        Duration elapsed = stopwatch.elapsed();
        totalAliasingDuration = totalAliasingDuration.plus(elapsed);
        log.info("getAliases took: " + elapsed.getSeconds() + " - " + elapsed.getNano());
        return aliases;
    }

    private BackwardQuery createQuery(Stmt stmt, SootMethod method, Value value) {
        final JimpleMethod jimpleMethod = JimpleMethod.of(method);
        final Statement statement = JimpleStatement.create(stmt, jimpleMethod);
        final JimpleVal val = new JimpleVal(value, jimpleMethod);
        Statement succ = statement.getMethod().getControlFlowGraph().getSuccsOf(statement).stream().findFirst().get();
        return BackwardQuery.make(new ControlFlowGraph.Edge(statement, succ), val);
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
        Set<AccessPath> aliases = AliasManager.getInstance().getAliases(stmt, method, base);
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
