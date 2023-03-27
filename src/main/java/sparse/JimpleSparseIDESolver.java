package sparse;

import com.google.common.collect.Table;
import heros.IDETabulationProblem;
import heros.InterproceduralCFG;
import heros.solver.Pair;
import heros.sparse.SparseCFGBuilder;
import heros.sparse.SparseIDESolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.PatchingChain;
import soot.SootMethod;
import soot.Unit;
import util.SortableCSVString;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class JimpleSparseIDESolver<D, V, I extends InterproceduralCFG<Unit, SootMethod>> extends SparseIDESolver<Unit, D, SootMethod, V, I> {
    private static final Logger logger = LoggerFactory.getLogger(JimpleSparseIDESolver.class);

    private static final String OUT_PUT_DIR = "./out";

    public JimpleSparseIDESolver(IDETabulationProblem<Unit, D, SootMethod, V, I> problem, SparseCFGBuilder<Unit, SootMethod, D> sparseCFGBuilder) {
        super(problem, sparseCFGBuilder);
    }

    public void solve(String targetClassName) {
        super.solve();
        this.dumpResults(targetClassName);
    }

    //private static Map<String, Set<String>> checkedMethods = new TreeMap<>();
    private static List<Pair<String, Set<String>>> checked = new ArrayList<>();

    public void addFinalResults(String entryMethod) {
        Iterator iter = this.val.cellSet().iterator();
        while (iter.hasNext()) {
            Table.Cell<Unit, D, ?> entry = (Table.Cell) iter.next();
            SootMethod method = this.icfg.getMethodOf(entry.getRowKey());
            Unit lastStmt = method.getActiveBody().getUnits().getLast();
            Set<String> results = new TreeSet<>();
            Map<D, V> res = this.resultsAt(lastStmt);
            for (Map.Entry<D, V> e : res.entrySet()) {
                if(!e.getKey().toString().contains("$stack") && !e.getKey().toString().contains("varReplacer")){
                    results.add(e.getKey().toString() + " - " + e.getValue());
                }
            }
            if(!results.isEmpty()){
                Pair pair = new Pair(method.getSignature(), results);
                if(!checked.contains(pair)){
                    checked.add(pair);
                }
            }
        }
    }

    public void dumpResults(String targetClassName) {
        File dir = new File(OUT_PUT_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File file = new File(OUT_PUT_DIR + File.separator + "sparse-" + targetClassName + ".csv");
        try (FileWriter writer = new FileWriter(file, true)) {
            for (Pair<String, Set<String>> pair : checked) {
                for(String res: pair.getO2()){
                    String str = pair.getO1() + ";" + res + System.lineSeparator();
                    writer.write(str);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}