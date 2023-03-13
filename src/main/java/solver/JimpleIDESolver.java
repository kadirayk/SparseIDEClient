package solver;

import com.google.common.collect.Table;
import heros.IDETabulationProblem;
import heros.InterproceduralCFG;
import heros.solver.IDESolver;

import java.io.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.PatchingChain;
import soot.SootMethod;
import soot.Unit;
import util.SortableCSVString;

public class JimpleIDESolver<D, V, I extends InterproceduralCFG<Unit, SootMethod>> extends IDESolver<Unit, D, SootMethod, V, I> {
    private static final Logger logger = LoggerFactory.getLogger(soot.jimple.toolkits.ide.JimpleIDESolver.class);

    private static final String OUT_PUT_DIR = "./out";

    public JimpleIDESolver(IDETabulationProblem<Unit, D, SootMethod, V, I> problem) {
        super(problem);
    }

    public void solve(String targetClassName) {
        super.solve();
        this.dumpResults(targetClassName);
    }

    private static Map<String, Set<String>> checkedMethods = new TreeMap<>();

    public void addFinalResults(String entryMethod) {
        Iterator iter = this.val.cellSet().iterator();
        while (iter.hasNext()) {
            Table.Cell<Unit, D, ?> entry = (Table.Cell) iter.next();
            SootMethod method = this.icfg.getMethodOf(entry.getRowKey());
            if (checkedMethods.containsKey(method.getSignature())) {
                continue;
            }
            Unit lastStmt = method.getActiveBody().getUnits().getLast();
            Set<String> results = new TreeSet<>();
            Map<D, V> res = this.resultsAt(lastStmt);
            for (Map.Entry<D, V> e : res.entrySet()) {
                if(!e.getKey().toString().contains("$stack") && !e.getKey().toString().contains("varReplacer")){
                    results.add(e.getKey().toString() + " - " + e.getValue());
                }
            }
            if(!results.isEmpty()){
                if(method.getSignature().contains("com.google.common.base.Joiner$3") && method.getSignature().contains("get(")){
                    System.out.println(entryMethod);
                }
                checkedMethods.put(method.getSignature(), results);
            }
        }
    }

    public void dumpResults(String targetClassName) {
        File dir = new File(OUT_PUT_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File file = new File(OUT_PUT_DIR + File.separator + "default-" + targetClassName + ".csv");
        try (FileWriter writer = new FileWriter(file, true)) {
            for (String key : checkedMethods.keySet()) {
                for (String res : checkedMethods.get(key)) {
                    String str = key + ";" + res + System.lineSeparator();
                    writer.write(str);

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
