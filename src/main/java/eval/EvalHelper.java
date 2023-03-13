package eval;

public class EvalHelper {

    private static String targetName;
    private static int threadCount = -1;
    private static int maxMethod = -1;
    private static long totalDuration = 0;
    private static long totalPropagationCount = 0;
    private static int actualMethodCount = 0;


    public static int getThreadCount() {
        return threadCount;
    }

    public static void setThreadCount(int threadCount) {
        EvalHelper.threadCount = threadCount;
    }

    public static long getTotalDuration() {
        return totalDuration;
    }

    public static void setTotalDuration(long totalDuration) {
        EvalHelper.totalDuration = totalDuration;
    }

    public static long getTotalPropagationCount() {
        return totalPropagationCount;
    }

    public static void setTotalPropagationCount(long totalPropagationCount) {
        EvalHelper.totalPropagationCount = totalPropagationCount;
    }

    public static int getActualMethodCount() {
        return actualMethodCount;
    }

    public static void setActualMethodCount(int actualMethodCount) {
        EvalHelper.actualMethodCount = actualMethodCount;
    }

    public static int getMaxMethod() {
        return maxMethod;
    }

    public static void setMaxMethod(int maxMethod) {
        EvalHelper.maxMethod = maxMethod;
    }

    public static String getTargetName() {
        return targetName;
    }

    public static void setTargetName(String targetName) {
        EvalHelper.targetName = targetName;
    }
}
