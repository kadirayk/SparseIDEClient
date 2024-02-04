package eval;

public class EvalHelper {

    private static String targetName;
    private static int threadCount = -1;
    private static int maxMethod = -1;
    private static long totalDuration = 0;

    private static long cg_construction_duration = 0;
    private static long totalPropagationCount = 0;
    private static int actualMethodCount = 0;

    private static String jarPath;

    private static  Main.CallgraphAlgorithm callgraphAlgorithm;

    private static String qilin_PTA;

    private static int number_of_cg_Edges;


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

    public static String getJarPath() {
        return jarPath;
    }

    public static void setJarPath(String jarPath) {
        EvalHelper.jarPath = jarPath;
    }

    public static Main.CallgraphAlgorithm getCallgraphAlgorithm() {
        return callgraphAlgorithm;
    }

    public static void setCallgraphAlgorithm(Main.CallgraphAlgorithm callgraphAlgorithm) {
        EvalHelper.callgraphAlgorithm = callgraphAlgorithm;
    }

    public static String getQilin_PTA() {
        return qilin_PTA;
    }

    public static void setQilin_PTA(String qilin_PTA) {
        EvalHelper.qilin_PTA = qilin_PTA;
    }

    public static int getNumber_of_cg_Edges() {
        return number_of_cg_Edges;
    }

    public static void setNumber_of_cg_Edges(int number_of_cg_Edges) {
        EvalHelper.number_of_cg_Edges = number_of_cg_Edges;
    }

    public static long getCg_construction_duration() {
        return cg_construction_duration;
    }

    public static void setCg_construction_duration(long cg_construction_duration) {
        EvalHelper.cg_construction_duration = EvalHelper.cg_construction_duration + cg_construction_duration;
    }
}
