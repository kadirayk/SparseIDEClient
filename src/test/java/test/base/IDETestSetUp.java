package test.base;

import analysis.IDELinearConstantAnalysisProblem;
import application.CallGraphApplication;
import boomerang.scene.jimple.BoomerangPretransformer;
import config.CallGraphAlgorithm;
import config.CallGraphConfig;
import eval.EvalHelper;
import eval.Main;
import metrics.CallGraphMetricsWrapper;
import solver.JimpleIDESolver;
import soot.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.options.Options;
import sparse.JimpleSparseIDESolver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class IDETestSetUp {

    protected static JimpleIDESolver<?, ?, ?> solver = null;
    protected static JimpleSparseIDESolver<?, ?, ?> sparseSolver = null;

    public static String soot_class_path = "";

    protected JimpleIDESolver<?, ?, ?> executeStaticAnalysis(String targetTestClassName) {
        setupSoot(targetTestClassName);
        registerSootTransformers();
        executeSootTransformers();
        if (solver == null) {
            throw new NullPointerException("Something went wrong solving the IDE problem!");
        }
        return solver;
    }

    protected CallGraph getCallGraph(String targetTestClassName){
        setupSoot(targetTestClassName);
        return returnCallGraph();
    }

    protected JimpleSparseIDESolver<?, ?, ?> executeSparseStaticAnalysis(String targetTestClassName) {
        setupSoot(targetTestClassName);
        registerSparseSootTransformers();
        executeSootTransformers();
        if (sparseSolver == null) {
            throw new NullPointerException("Something went wrong sparsely solving the IDE problem!");
        }
        return sparseSolver;
    }

    private CallGraph returnCallGraph(){
        PackManager.v().getPack("cg").apply();
        return CallGraphApplication.generateCallGraph(Scene.v(),constructCallGraphConfig()).getCallGraph();
    }

    private void executeSootTransformers() {
        //Apply all necessary packs of soot. This will execute the respective Transformer
        PackManager.v().getPack("cg").apply();
        CallGraphMetricsWrapper callGraphMetrics = CallGraphApplication.generateCallGraph(Scene.v(),constructCallGraphConfig());
        Scene.v().setCallGraph(callGraphMetrics.getCallGraph());
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

    private CallGraphConfig constructCallGraphConfig() {
        CallGraphConfig callGraphConfig = CallGraphConfig.getInstance();
        callGraphConfig.setAppPath(EvalHelper.getJarPath());
        SootMethod entryPointMethod = getEntryPointMethod();
        callGraphConfig.setMainClass(entryPointMethod.getName());
        callGraphConfig.setSingle_entry(true);
        CallGraphAlgorithm callgraphAlgorithm = configureCallgraph(EvalHelper.getCallgraphAlgorithm());
        callGraphConfig.setCallGraphAlgorithm(callgraphAlgorithm);
        callGraphConfig.setIsSootSceneProvided(true);
        if (callgraphAlgorithm == CallGraphAlgorithm.QILIN) {
            callGraphConfig.setQilinPta(EvalHelper.getQilin_PTA());
        }
        return callGraphConfig;
    }

    protected CallGraphAlgorithm configureCallgraph(Main.CallgraphAlgorithm callgraphAlgorithm) {
        // Configure the callgraph algorithm
        CallGraphAlgorithm callGraphAlgorithm;
        switch (callgraphAlgorithm) {
            case QILIN:
                callGraphAlgorithm = CallGraphAlgorithm.QILIN;
                break;
            case AutomaticSelection:
            case SPARK:
                callGraphAlgorithm = CallGraphAlgorithm.SPARK;
                break;
            case GEOM:
                callGraphAlgorithm = CallGraphAlgorithm.GEOM;
                break;
            case CHA:
                callGraphAlgorithm = CallGraphAlgorithm.CHA;
                break;
            case RTA:
                callGraphAlgorithm = CallGraphAlgorithm.RTA;
                break;
            case VTA:
                callGraphAlgorithm = CallGraphAlgorithm.VTA;
                break;
            default:
                throw new RuntimeException("Invalid callgraph algorithm");
        }
        return callGraphAlgorithm;
    }


    protected abstract Transformer createAnalysisTransformer();

    protected abstract Transformer createSparseAnalysisTransformer();

    /*
     * This method provides the options to soot to analyse the respecive
     * classes.
     */
    private void setupSoot(String targetTestClassName) {
        G.reset();
        String userdir = System.getProperty("user.dir");
		String sootCp = userdir + File.separator + "target" + File.separator + "test-classes"+ File.pathSeparator + "lib"+File.separator+"rt.jar";
        Options.v().set_soot_classpath(soot_class_path);

        // We want to perform a whole program, i.e. an interprocedural analysis.
        // We construct a basic CHA call graph for the program
        Options.v().set_whole_program(true);
        Options.v().setPhaseOption("cg.cha", "on");
        Options.v().setPhaseOption("cg", "all-reachable:true");

        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().setPhaseOption("jb.ls", "enabled:false");
        Options.v().set_prepend_classpath(false);

        Scene.v().addBasicClass("java.lang.StringBuilder");
        SootClass c = Scene.v().forceResolve(targetTestClassName, SootClass.BODIES);
        if (c != null) {
            c.setApplicationClass();
        }
        Scene.v().loadNecessaryClasses();
    }


    protected SootMethod getEntryPointMethod() {
        for (SootClass c : Scene.v().getApplicationClasses()) {
            for (SootMethod m : c.getMethods()) {
                if (!m.hasActiveBody()) {
                    continue;
                }
                if (m.getName().equals("entryPoint") || m.toString().contains("void main(java.lang.String[])")) {
                    return m;
                }
            }
        }
        throw new IllegalArgumentException("Method does not exist in scene!");
    }

}
