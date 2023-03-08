package eval;

import analysis.IDELinearConstantAnalysisProblem;
import analysis.data.DFF;
import boomerang.scene.jimple.BoomerangPretransformer;
import heros.solver.Pair;
import heros.sparse.SparseCFGBuilder;
import solver.JimpleIDESolver;
import soot.*;
import soot.jimple.DefinitionStmt;
import soot.jimple.IntConstant;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.options.Options;
import sparse.CPAJimpleSparseCFGBuilder;
import sparse.JimpleSparseIDESolver;

import java.io.File;
import java.util.*;

public class SetUp {

    private static JimpleIDESolver<?, ?, ?> solver;
    private static JimpleSparseIDESolver<?, ?, ?> sparseSolver;

    private static List<SootMethod> entryMethods;

    public long defaultPropCount = 0;
    public long sparsePropCount = 0;

    protected void executeStaticAnalysis(String jarPath) {
        setupSoot(jarPath);
        registerSootTransformers();
        executeSootTransformers();
    }

    protected void executeSparseStaticAnalysis(String jarPath) {
        setupSoot(jarPath);
        registerSparseSootTransformers();
        executeSootTransformers();
    }

    private void executeSootTransformers() {
        //Apply all necessary packs of soot. This will execute the respective Transformer
        PackManager.v().getPack("cg").apply();
        // Must have for Boomerang
        BoomerangPretransformer.v().reset();
        BoomerangPretransformer.v().apply();
        PackManager.v().getPack("wjtp").apply();
    }

    private void registerSootTransformers() {
        Transform transform = new Transform("wjtp.ifds", createAnalysisTransformer());
        PackManager.v().getPack("wjtp").add(transform);
    }

    private void registerSparseSootTransformers() {
        Transform transform = new Transform("wjtp.ifds", createSparseAnalysisTransformer());
        PackManager.v().getPack("wjtp").add(transform);
    }

    protected Transformer createAnalysisTransformer() {
        return new SceneTransformer() {
            @Override
            protected void internalTransform(String phaseName, Map<String, String> options) {
                JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG(false);
                for (SootMethod method : entryMethods) {
                    System.out.println("started solving from: " + method.getSignature());
                    IDELinearConstantAnalysisProblem problem = new IDELinearConstantAnalysisProblem(icfg, method, EvalHelper.getThreadCount());
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    JimpleIDESolver<?, ?, ?> solver = new JimpleIDESolver<>(problem);
                    solver.solve();
                    getResult(solver, method);
                }
            }
        };
    }

    protected Transformer createSparseAnalysisTransformer() {
        return new SceneTransformer() {
            @Override
            protected void internalTransform(String phaseName, Map<String, String> options) {
                JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG(false);
                for (SootMethod method : entryMethods) {
                    System.out.println("sparse solving " + method.getSignature());
                    IDELinearConstantAnalysisProblem problem = new IDELinearConstantAnalysisProblem(icfg, method, EvalHelper.getThreadCount());
                    SparseCFGBuilder sparseCFGBuilder = new CPAJimpleSparseCFGBuilder(true);
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    JimpleSparseIDESolver<?, ?, ?> solver = new JimpleSparseIDESolver<>(problem, sparseCFGBuilder);
                    solver.solve();
                    getResult(solver, method);
                }
            }
        };
    }

    /*
     * This method provides the options to soot to analyse the respecive
     * classes.
     */
    private void setupSoot(String jarPath) {
        G.reset();
        String userdir = System.getProperty("user.dir");
        String sootCp = jarPath + File.pathSeparator + "lib" + File.separator + "rt.jar";
        Options.v().set_soot_classpath(sootCp);
        Options.v().set_process_dir(Collections.singletonList(jarPath));

        // We want to perform a whole program, i.e. an interprocedural analysis.
        // We construct a basic CHA call graph for the program
        Options.v().set_whole_program(true);
        Options.v().setPhaseOption("cg.cha", "on");
        Options.v().setPhaseOption("cg", "all-reachable:true");

        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().set_prepend_classpath(false);

        Scene.v().addBasicClass("java.lang.StringBuilder");
//        SootClass c = Scene.v().forceResolve("com.google.common.io.MultiReader", SootClass.BODIES);
//        if (c != null) {
//            c.setApplicationClass();
//        }
        Scene.v().loadNecessaryClasses();
//        for (SootClass sc : Scene.v().getApplicationClasses()) {
//            Scene.v().forceResolve(sc.getName(), SootClass.BODIES);
//        }
//        Scene.v().loadNecessaryClasses();
        entryMethods = getEntryPointMethods();
    }


    private boolean isPublicAPI(SootMethod method){
        return !method.isStatic() && method.isPublic() && !method.isAbstract() && !method.isConstructor() && !method.isNative() && !method.isStaticInitializer();
    }

    protected List<SootMethod> getEntryPointMethods() {
        List<SootMethod> methods = new ArrayList<>();
        l1:
        for (SootClass c : Scene.v().getApplicationClasses()) {
            for (SootMethod m : c.getMethods()) {
                MethodSource source = m.getSource();
                if(source!=null){
                    if(isPublicAPI(m)){
                        m.retrieveActiveBody();
                        if (m.hasActiveBody()) {
                            if(m.getReturnType() instanceof IntegerType && m.getParameterTypes().stream().anyMatch(t->t instanceof IntegerType && !t.equals(BooleanType.v()))){
                                UnitPatchingChain units = m.getActiveBody().getUnits();
                                for (Unit unit : units) {
                                    if(unit instanceof DefinitionStmt){
                                        DefinitionStmt assign = (DefinitionStmt) unit;
                                        Value rhs = assign.getRightOp();
                                        if(rhs instanceof IntConstant ){
                                            methods.add(m);
                                            if(methods.size()==EvalHelper.getMaxMethod()){
                                                break l1;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if(!methods.isEmpty()){
            System.out.println(methods.size() + " methods will be used as entry points");
            EvalHelper.setActualMethodCount(methods.size());
            return methods;
        }
        throw new RuntimeException("no entry methods found to start");
    }

    public Set<Pair<String, Integer>> getResult(Object analysis, SootMethod method) {
        //SootMethod m = getEntryPointMethod();
        Map<DFF, Integer> res = null;
        Set<Pair<String, Integer>> result = new HashSet<>();
        if (analysis instanceof JimpleIDESolver) {
            JimpleIDESolver solver = (JimpleIDESolver) analysis;
            res = (Map<DFF, Integer>) solver.resultsAt(method.getActiveBody().getUnits().getLast());
            defaultPropCount += solver.propagationCount;
        } else if (analysis instanceof JimpleSparseIDESolver) {
            JimpleSparseIDESolver solver = (JimpleSparseIDESolver) analysis;
            res = (Map<DFF, Integer>) solver.resultsAt(method.getActiveBody().getUnits().getLast());
            sparsePropCount += solver.propagationCount;
        }
        for (Map.Entry<DFF, Integer> e : res.entrySet()) {
            Pair<String, Integer> pair = new Pair<>(e.getKey().toString(), e.getValue());
            result.add(pair);
        }
        return result;
    }

}

