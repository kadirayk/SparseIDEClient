package eval;

import heros.sparse.SparseCFGCache;
import heros.sparse.SparseCFGQueryStat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class EvalPrinter {
    private static final String OUT_PUT_DIR = "./results";
    private static final String FILE = "sparseide_eval.csv";

    private final String targetProgram;
    private final String solver;
    private int threadCount = 0;
    private long totalDuration = 0;
    private long totalPropagationCount = 0;
    private int methodCount = 0;
    private long sparseCFGBuildtime = 0;
    private long scfgBuildCount = 0;


    public EvalPrinter(String solver) {
        this.solver = solver;
        this.targetProgram = EvalHelper.getTargetName();
        this.threadCount = EvalHelper.getThreadCount();
        this.totalDuration = EvalHelper.getTotalDuration();
        this.totalPropagationCount = EvalHelper.getTotalPropagationCount();
        this.methodCount = EvalHelper.getActualMethodCount();
        if(solver.equalsIgnoreCase("sparse")){
            handleSparseQueryStats();
        }
    }

    private void handleSparseQueryStats() {
        List<SparseCFGQueryStat> queryStats = SparseCFGCache.getQueryStats();
        for (SparseCFGQueryStat queryStat : queryStats) {
            if(!queryStat.isRetrievedFromCache()){
                sparseCFGBuildtime += queryStat.getDuration().toMillis();
                scfgBuildCount++;
            }
        }
    }

    public void generate() {
        File dir = new File(OUT_PUT_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File file = new File(OUT_PUT_DIR + File.separator + FILE);
        if(!file.exists()){
            try (FileWriter writer = new FileWriter(file)) {
                String str = "jar" +
                        "," +
                        "solver" +
                        "," +
                        "thread" +
                        "," +
                        "runtime" +
                        "," +
                        "prop" +
                        "," +
                        "method" +
                        "," +
                        "SCFGConst" +
                        "," +
                        "SCFGCount" +
                        "," +
                        "mem" +
                        "," +
                        "cg_edges" +
                        "," +
                        "cg_name" +
                        "," +
                        "cg_time" +
                        "," +
                        "num_methods_propagated" +
                        "," +
                        "reachable_methods" +
                        System.lineSeparator();
                writer.write(str);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (FileWriter writer = new FileWriter(file, true)) {
            String str = targetProgram +
                    "," +
                    solver +
                    "," +
                    threadCount +
                    "," +
                    totalDuration +
                    "," +
                    totalPropagationCount +
                    "," +
                    methodCount +
                    "," +
                    sparseCFGBuildtime +
                    "," +
                    scfgBuildCount +
                    "," +
                    getMemoryUsage() +
                    "," +
                    EvalHelper.getNumber_of_cg_Edges() +
                    "," +
                    getCg_name() +
                    "," +
                    EvalHelper.getCg_construction_duration() +
                    ","+
                    EvalHelper.getNumber_of_methods_propagated() +
                    "," +
                    EvalHelper.getNumber_of_reachable_methods() +
                    System.lineSeparator();
            writer.write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getCg_name(){
        if(EvalHelper.getCallgraphAlgorithm() != Main.CallgraphAlgorithm.QILIN){
            return EvalHelper.getCallgraphAlgorithm().toString();
        }
        else{
            return EvalHelper.getQilin_PTA();
        }
    }


    /**
     * in MB
     * @return
     */
    private static int getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return Math.round((runtime.totalMemory() - runtime.freeMemory()) / (1024*1024));
    }


}
