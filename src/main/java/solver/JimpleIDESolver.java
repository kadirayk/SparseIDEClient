package solver;

import com.google.common.collect.Table;
import heros.IDETabulationProblem;
import heros.InterproceduralCFG;
import heros.solver.IDESolver;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.PatchingChain;
import soot.SootMethod;
import soot.Unit;
import util.SortableCSVString;

public class JimpleIDESolver<D, V, I extends InterproceduralCFG<Unit, SootMethod>> extends IDESolver<Unit, D, SootMethod, V, I> {
    private static final Logger logger = LoggerFactory.getLogger(soot.jimple.toolkits.ide.JimpleIDESolver.class);

    public JimpleIDESolver(IDETabulationProblem<Unit, D, SootMethod, V, I> problem) {
        super(problem);
    }

    public void solve(String targetClassName) {
        super.solve();
        this.dumpResults(targetClassName);
    }

    public void dumpResults(String targetClassName) {
        try {
            String fileName = targetClassName==null || targetClassName.isEmpty() ? "ideSolverDump" : targetClassName;
            PrintWriter out = new PrintWriter(new FileOutputStream("out/" + fileName + "-" + System.currentTimeMillis()%10000 + ".csv"));
            List<SortableCSVString> res = new ArrayList();
            Iterator var3 = this.val.cellSet().iterator();

            while(var3.hasNext()) {
                Table.Cell<Unit, D, ?> entry = (Table.Cell)var3.next();
                SootMethod methodOf = this.icfg.getMethodOf(entry.getRowKey());
                PatchingChain<Unit> units = methodOf.getActiveBody().getUnits();
                int i = 0;
                Iterator var8 = units.iterator();

                while(true) {
                    if (var8.hasNext()) {
                        Unit unit = (Unit) var8.next();
                        if (unit != entry.getRowKey()) {
                            ++i;
                            continue;
                        }
                    }

                    res.add(new SortableCSVString(methodOf + ";" + entry.getRowKey() + "@" + i + ";" + entry.getColumnKey() + ";" + entry.getValue(), i));
                    break;
                }
            }

            Collections.sort(res);
            var3 = res.iterator();

            while(var3.hasNext()) {
                SortableCSVString string = (SortableCSVString)var3.next();
                out.println(string.value.replace("\"", "'"));
            }

            out.flush();
            out.close();
        } catch (FileNotFoundException var10) {
            logger.error(var10.getMessage(), var10);
        }

    }
}
