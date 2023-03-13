package eval;

import com.google.common.base.Stopwatch;

import java.io.File;
import java.text.MessageFormat;
import java.time.Duration;

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
        if(args.length>3){
            numThreads = Integer.parseInt(args[3]); // thread count
        }

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
        Duration elapsed = stopwatch.elapsed();
        EvalHelper.setTotalDuration(elapsed.toMillis());
        new EvalPrinter(solver).generate();
    }

    private static String getJarName(String fullpath){
        int start = fullpath.lastIndexOf(File.separator);
        int endDot = fullpath.lastIndexOf(".");
        int endDash = fullpath.lastIndexOf("-");
        int latest = endDot<endDash ? endDash : endDot;
        return fullpath.substring(start + 1, latest);
    }

}
