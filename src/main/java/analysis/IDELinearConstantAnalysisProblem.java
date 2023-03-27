package analysis;

import analysis.data.DFF;
import analysis.edgefunctions.CPANormalEdgeFunctionProvider;
import analysis.edgefunctions.normal.IntegerTop;
import analysis.flowfunctions.CPACallFlowFunctionProvider;
import analysis.flowfunctions.CPANormalFlowFunctionProvider;
import analysis.flowfunctions.CPAReturnFlowFunctionProvider;
import heros.*;
import heros.edgefunc.EdgeIdentity;
import heros.flowfunc.Identity;
import soot.*;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.ide.DefaultJimpleIDETabulationProblem;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import util.CFGUtil;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class IDELinearConstantAnalysisProblem extends DefaultJimpleIDETabulationProblem<DFF, Integer, InterproceduralCFG<Unit, SootMethod>> {

    protected InterproceduralCFG<Unit, SootMethod> icfg;

    private boolean isSparse;

    private int numThreads=-1;

    private SootMethod entryMethod;

    public final static Integer TOP = Integer.MIN_VALUE; // Unknown

    public final static Integer BOTTOM = Integer.MAX_VALUE; // Not Constant

    @Override
    public int numThreads() {
        return numThreads!=-1 ? numThreads : super.numThreads();
    }

    public IDELinearConstantAnalysisProblem(InterproceduralCFG<Unit, SootMethod> icfg, SootMethod entry, int numThreads) {
        super(icfg);
        this.icfg = icfg;
        this.entryMethod = entry;
        this.isSparse = false;
    }

    public IDELinearConstantAnalysisProblem(InterproceduralCFG<Unit, SootMethod> icfg) {
        super(icfg);
        this.icfg = icfg;
    }

    @Override
    protected EdgeFunction<Integer> createAllTopFunction() {
        return IntegerTop.v();
    }

    @Override
    protected MeetLattice<Integer> createMeetLattice() {
        return new MeetLattice<Integer>() {
            @Override
            public Integer topElement() {
                return TOP;
            }

            @Override
            public Integer bottomElement() {
                return BOTTOM;
            }

            @Override
            public Integer meet(Integer left, Integer right) {
                if(left==TOP){
                    return right;
                }
                if(right==TOP){
                    return left;
                }
                if(left==BOTTOM){
                    return left;
                }
                if(right==BOTTOM){
                    return right;
                }
                if(left==right){
                    return left;
                }else{
                    return BOTTOM;
                }
            }
        };
    }

    @Override
    protected FlowFunctions<Unit, DFF, SootMethod> createFlowFunctionsFactory() {
        return new FlowFunctions<Unit, DFF, SootMethod>() {
            @Override
            public FlowFunction<DFF> getNormalFlowFunction(Unit curr, Unit succ) {
                CPANormalFlowFunctionProvider ffp = new CPANormalFlowFunctionProvider(icfg.getMethodOf(curr), curr, zeroValue());
                return ffp.getFlowFunction();
            }

            @Override
            public FlowFunction<DFF> getCallFlowFunction(Unit callStmt, SootMethod dest) {
                CPACallFlowFunctionProvider ffp = new CPACallFlowFunctionProvider(callStmt, dest, zeroValue());
                return ffp.getFlowFunction();
            }

            @Override
            public FlowFunction<DFF> getReturnFlowFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {
                CPAReturnFlowFunctionProvider ffp = new CPAReturnFlowFunctionProvider(callSite, exitStmt, icfg.getMethodOf(callSite));
                return ffp.getFlowFunction();
            }

            @Override
            public FlowFunction<DFF> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
                return Identity.v();
            }
        };
    }

    @Override
    protected EdgeFunctions<Unit, DFF, SootMethod, Integer> createEdgeFunctionsFactory() {
        return new EdgeFunctions<Unit, DFF, SootMethod, Integer>() {
            @Override
            public EdgeFunction<Integer> getNormalEdgeFunction(Unit src, DFF srcNode, Unit tgt, DFF tgtNode) {
                CPANormalEdgeFunctionProvider efp = new CPANormalEdgeFunctionProvider(icfg.getMethodOf(src), src, srcNode, tgtNode, zeroValue());
                return efp.getEdgeFunction();
            }

            @Override
            public EdgeFunction<Integer> getCallEdgeFunction(Unit callStmt, DFF srcNode, SootMethod destinationMethod, DFF destNode) {
                return EdgeIdentity.v();
            }

            @Override
            public EdgeFunction<Integer> getReturnEdgeFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt, DFF exitNode, Unit returnSite, DFF retNode) {
                return EdgeIdentity.v();
            }

            @Override
            public EdgeFunction<Integer> getCallToReturnEdgeFunction(Unit callStmt, DFF callNode, Unit returnSite, DFF returnSideNode) {
                return EdgeIdentity.v();
            }
        };
    }

    @Override
    protected DFF createZeroValue() {
        return DFF.asDFF(new JimpleLocal("<<zero>>", NullType.v()));
    }

    @Override
    public Map<Unit, Set<DFF>> initialSeeds() {
        if(entryMethod!=null){
            DirectedGraph<Unit> unitGraph = new BriefUnitGraph(entryMethod.getActiveBody());
            return DefaultSeeds.make(Collections.singleton(CFGUtil.getHead(unitGraph)), zeroValue());
        }
        for (SootClass c : Scene.v().getApplicationClasses()) {
            for (SootMethod m : c.getMethods()) {
                if (!m.hasActiveBody()) {
                    continue;
                }
                if (m.getName().equals("entryPoint")) {
                    DirectedGraph<Unit> unitGraph = new BriefUnitGraph(m.getActiveBody());
                    SootMethod methodOf = icfg.getMethodOf(CFGUtil.getHead(unitGraph));
                    //System.out.println(methodOf.getActiveBody());
                    return DefaultSeeds.make(Collections.singleton(CFGUtil.getHead(unitGraph)), zeroValue());
                }
            }
        }
        throw new IllegalStateException("scene does not contain 'entryPoint'");
    }



}
