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
    private float initalStmtCount = 0;
    private float finalStmtCount = 0;


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
                //if (queryStat.getInitialStmtCount() > 0 && queryStat.getFinalStmtCount() > 0) { // check for cache retrieve
                    initalStmtCount += queryStat.getInitialStmtCount();
                    finalStmtCount += queryStat.getFinalStmtCount();
                    scfgBuildCount++;
                //}
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
                StringBuilder str = new StringBuilder();
                str.append("jar");
                str.append(",");
                str.append("solver");
                str.append(",");
                str.append("thread");
                str.append(",");
                str.append("runtime");
                str.append(",");
                str.append("prop");
                str.append(",");
                str.append("method");
                str.append(",");
                str.append("SCFGConst");
                str.append(",");
                str.append("SCFGCount");
                str.append(",");
                str.append("DoS");
                str.append(System.lineSeparator());
                writer.write(str.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (FileWriter writer = new FileWriter(file, true)) {
            StringBuilder str = new StringBuilder();
            str.append(targetProgram);
            str.append(",");
            str.append(solver);
            str.append(",");
            str.append(threadCount);
            str.append(",");
            str.append(totalDuration);
            str.append(",");
            str.append(totalPropagationCount);
            str.append(",");
            str.append(methodCount);
            str.append(",");
            str.append(sparseCFGBuildtime);
            str.append(",");
            str.append(scfgBuildCount);
            str.append(",");
            str.append(degreeOfSparsification());
            str.append(System.lineSeparator());
            writer.write(str.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String degreeOfSparsification(){
        if(finalStmtCount!=0){
            return String.format("%.2f",(initalStmtCount-finalStmtCount)/initalStmtCount);
        }
        return "0";
    }

}
