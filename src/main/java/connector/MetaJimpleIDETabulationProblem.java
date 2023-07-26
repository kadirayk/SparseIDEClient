package connector;

import heros.InterproceduralCFG;
import heros.template.DefaultIDETabulationProblem;
import soot.SootMethod;
import soot.Unit;

public abstract class MetaJimpleIDETabulationProblem<D, V, I extends InterproceduralCFG<Unit, SootMethod>, X> extends DefaultIDETabulationProblem<Unit, D, SootMethod, V, I, X> {
    public MetaJimpleIDETabulationProblem(I icfg) {
        super(icfg);
    }
}