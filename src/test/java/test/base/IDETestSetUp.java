package test.base;

import boomerang.scene.jimple.BoomerangPretransformer;
import solver.JimpleIDESolver;
import soot.*;
import soot.options.Options;
import sparse.JimpleSparseIDESolver;

import java.io.File;

public abstract class IDETestSetUp {

    protected static JimpleIDESolver<?, ?, ?, ?> solver = null;
    protected static JimpleSparseIDESolver<?, ?, ?, ?> sparseSolver = null;

    protected JimpleIDESolver<?, ?, ?, ?> executeStaticAnalysis(String targetTestClassName) {
        setupSoot(targetTestClassName);
        registerSootTransformers();
        Scene.v().getSootClass("target.constant.Assignment3");
        executeSootTransformers();
        if (solver == null) {
            throw new NullPointerException("Something went wrong solving the IDE problem!");
        }
        return solver;
    }

    protected JimpleSparseIDESolver<?, ?, ?, ?> executeSparseStaticAnalysis(String targetTestClassName) {
        setupSoot(targetTestClassName);
        registerSparseSootTransformers();
        executeSootTransformers();
        if (sparseSolver == null) {
            throw new NullPointerException("Something went wrong sparsely solving the IDE problem!");
        }
        return sparseSolver;
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
        Options.v().set_soot_classpath(sootCp);

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
