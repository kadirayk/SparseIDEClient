package sparse;

import analysis.data.DFF;
import heros.sparse.SparseCFG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Unit;
import soot.toolkits.graph.DirectedGraph;

import java.util.*;


public class JimpleDefaultSparseCFG implements SparseCFG<Unit, DFF> {

    private static Logger log = LoggerFactory.getLogger(JimpleDefaultSparseCFG.class);

    private DirectedGraph<Unit> graph;
    private DFF d; // which dff this SCFG belongs to
    private Map<Unit, Unit> jumps;
    private int stmtCount;

    public JimpleDefaultSparseCFG(DFF d, DirectedGraph<Unit> graph, Map<Unit, Unit> jumps, int stmtCount) {
        this.d = d;
        this.graph = graph;
        this.jumps = jumps;
        this.stmtCount = stmtCount;
    }

    @Override
    public void add(Unit node) {
    }

    @Override
    public List<Unit> getNextUses(Unit node) {
        if(jumps!=null){
            Unit units = this.jumps.get(node);
            if(units!=null){
                return Collections.singletonList(units);
            }
        }
        return graph.getSuccsOf(node);
    }

    @Override
    public DFF getD() {
        return this.d;
    }

    // TODO: Broken
    public String toString(){
        StringBuilder str = new StringBuilder(d.getValue().toString()).append(" :\n");
        List<Unit> heads = graph.getHeads();
        for (Unit head : heads) {
            str.append("head: ").append(head).append("\n");
            List<Unit> nextUses = getNextUses(head);
            while(!nextUses.isEmpty()){
                if (nextUses.size()>1){
                    System.out.println("Wow big");
                }
                for (Unit nextUs : nextUses) {
                    str.append(nextUs).append("\n");
                    nextUses = getNextUses(nextUs);
                }
            }
        }
        return str.toString();
    }

}
