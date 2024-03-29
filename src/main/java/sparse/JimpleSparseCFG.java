package sparse;

import analysis.data.DFF;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import heros.sparse.SparseCFG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class JimpleSparseCFG implements SparseCFG<Unit, DFF> {

    private static Logger log = LoggerFactory.getLogger(JimpleSparseCFG.class);

    private MutableGraph<Unit> graph;
    private DFF d; // which dff this SCFG belongs to

    public JimpleSparseCFG(DFF d, MutableGraph<Unit> graph) {
        this.d = d;
        this.graph = graph;
    }

    public synchronized boolean addEdge(Unit node, Unit succ){
        return graph.putEdge(node, succ);
    }

    public Set<Unit> getSuccessors(Unit node){
        if(graph.nodes().contains(node)){
            return graph.successors(node);
        }
        return Collections.emptySet();
    }

    @Override
    public void add(Unit node) {
        // should not have same node twice
    }

    @Override
    public List<Unit> getNextUses(Unit node) {
        Set<Unit> successors = getSuccessors(node);
        return new ArrayList<>(successors);
    }

    @Override
    public DFF getD() {
        return this.d;
    }


    public MutableGraph<Unit>  getGraph(){
        return this.graph;
    }

}
