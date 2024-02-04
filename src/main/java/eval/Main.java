package eval;

import com.google.common.base.Stopwatch;
import config.CallGraphAlgorithm;

import java.io.File;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class Main {

    /**
     * Run with args: "input/maventop/guava-18.0.jar" sparse 10 1
     * @param args
     */
    public static void main(String[] args){
        String jarPath = args[0]; // path to input jar
        String solver = args[1]; // solver: default or sparse
        int maxMethods = Integer.parseInt(args[2]); // max number of methods to analyze
        int numThreads = Runtime.getRuntime().availableProcessors(); // thread count
        try {
            CallgraphAlgorithm callgraphAlgorithm = parseCallgraphAlgorithm(args[3]);
            EvalHelper.setCallgraphAlgorithm(callgraphAlgorithm);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if(args.length > 4){
            EvalHelper.setQilin_PTA(args[4]);
        }
        if(args.length>5){
            numThreads = Integer.parseInt(args[5]); // thread count
        }

        EvalHelper.setJarPath(jarPath);
        EvalHelper.setTargetName(getJarName(jarPath));
        EvalHelper.setMaxMethod(maxMethods);
        EvalHelper.setThreadCount(numThreads);
        String msg = MessageFormat.format("Running {0} - {1} solver - {2} threads", EvalHelper.getTargetName(), solver, numThreads);
        System.out.println(msg);

        SetUp setUp = new SetUp();
        Stopwatch stopwatch = Stopwatch.createStarted();
        if(solver.equalsIgnoreCase("default")){
            setUp.executeStaticAnalysis(jarPath);
            EvalHelper.setTotalPropagationCount(setUp.defaultPropCount); // clean later
        }else if (solver.equalsIgnoreCase("sparse")){
            setUp.executeSparseStaticAnalysis(jarPath);
            EvalHelper.setTotalPropagationCount(setUp.sparsePropCount);
        }
        Duration elapsed = Duration.ofDays(stopwatch.elapsed(TimeUnit.MILLISECONDS));
        EvalHelper.setTotalDuration(elapsed.toMillis());
//        new EvalPrinter(solver).generate();
    }

    private static String getJarName(String fullpath){
        int start = fullpath.lastIndexOf(File.separator);
        int endDot = fullpath.lastIndexOf(".");
        int endDash = fullpath.lastIndexOf("-");
        int latest = endDot<endDash ? endDash : endDot;
        return fullpath.substring(start + 1, latest);
    }

    public static CallgraphAlgorithm parseCallgraphAlgorithm(String algo) throws Exception {
        if (algo.equalsIgnoreCase("AUTO"))
            return CallgraphAlgorithm.AutomaticSelection;
        else if (algo.equalsIgnoreCase("CHA"))
            return CallgraphAlgorithm.CHA;
        else if (algo.equalsIgnoreCase("VTA"))
            return CallgraphAlgorithm.VTA;
        else if (algo.equalsIgnoreCase("RTA"))
            return CallgraphAlgorithm.RTA;
        else if (algo.equalsIgnoreCase("SPARK"))
            return CallgraphAlgorithm.SPARK;
        else if (algo.equalsIgnoreCase("GEOM"))
            return CallgraphAlgorithm.GEOM;
        else if (algo.equalsIgnoreCase("QILIN"))
            return CallgraphAlgorithm.QILIN;
        else {
            System.err.printf("Invalid callgraph algorithm: %s%n", algo);
            throw new Exception();
        }
    }

    public enum CallgraphAlgorithm {
        AutomaticSelection, CHA, VTA, RTA, SPARK, GEOM, OnDemand, QILIN
    }

}
