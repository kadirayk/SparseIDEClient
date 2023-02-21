package test.constant;

import analysis.IDELinearConstantAnalysisProblem;
import heros.InterproceduralCFG;
import heros.solver.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.fail;

import heros.sparse.SparseCFGBuilder;
import org.junit.Test;
import solver.JimpleIDESolver;
import soot.*;

import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import sparse.JimpleSparseCFGBuilder;
import sparse.JimpleSparseIDESolver;
import target.constant.FunctionCall2;
import target.constant.SimpleAssignment;
import target.constant.SimpleAssignment2;
import target.constant.SimpleAssignment3;
import target.constant.SimpleAssignment4;
import target.constant.SimpleAssignment5;
import test.base.IDETestSetUp;

public class ConstantPropagationAnalysisTest extends IDETestSetUp {

    @Override
    protected Transformer createAnalysisTransformer() {
        return new SceneTransformer() {
            @Override
            protected void internalTransform(String phaseName, Map<String, String> options) {
                JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG(false);
                IDELinearConstantAnalysisProblem problem = new IDELinearConstantAnalysisProblem(icfg);
                @SuppressWarnings({"rawtypes", "unchecked"})
                JimpleIDESolver<?, ?, ?> solver = new JimpleIDESolver<>(problem);
                solver.solve();
                IDETestSetUp.solver = solver;
            }
        };
    }

    @Override
    protected Transformer createSparseAnalysisTransformer() {
        return new SceneTransformer() {
            @Override
            protected void internalTransform(String phaseName, Map<String, String> options) {
                JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG(false);
                IDELinearConstantAnalysisProblem problem = new IDELinearConstantAnalysisProblem(icfg);
                SparseCFGBuilder sparseCFGBuilder = new JimpleSparseCFGBuilder(true);
                @SuppressWarnings({"rawtypes", "unchecked"})
                JimpleSparseIDESolver<?, ?, ?> solver = new JimpleSparseIDESolver<>(problem, sparseCFGBuilder);
                solver.solve();
                IDETestSetUp.sparseSolver = solver;
            }
        };
    }

    void checkResultsAtLastStatement(JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis,
                                     List<Pair<String, Integer>> expectedResult) {
        SootMethod m = getEntryPointMethod();
        Map<?, ?> res = analysis.resultsAt(m.getActiveBody().getUnits().getLast());
        int correctResultCounter = 0;
        for (Pair<String, Integer> expected : expectedResult) {
            for (Map.Entry<?, ?> entry : res.entrySet()) {
                Map.Entry<Local, Integer> e = (Map.Entry<Local, Integer>) entry;
                if (expected.getO1().equals(e.getKey().getName()) && expected.getO2().intValue() == e.getValue().intValue()) {
                    correctResultCounter++;
                }
            }
        }
        if (correctResultCounter != expectedResult.size()) {
            fail("results are not complete or correct");
        }
    }

    void checkResultsAtLastStatement(JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis,
                                     JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis,
                                     List<Pair<String, Integer>> expectedResult) {
        SootMethod m = getEntryPointMethod();
        Map<?, ?> res = analysis.resultsAt(m.getActiveBody().getUnits().getLast());
        Map<?, ?> sparseRes = sparseAnalysis.resultsAt(m.getActiveBody().getUnits().getLast());
        int correctResultCounter = 0;
        for (Pair<String, Integer> expected : expectedResult) {
            for (Map.Entry<?, ?> entry : res.entrySet()) {
                Map.Entry<Local, Integer> e = (Map.Entry<Local, Integer>) entry;
                if (expected.getO1().equals(e.getKey().getName()) && expected.getO2().intValue() == e.getValue().intValue()) {
                    correctResultCounter++;
                }
            }
            for (Map.Entry<?, ?> entry : sparseRes.entrySet()) {
                Map.Entry<Local, Integer> e = (Map.Entry<Local, Integer>) entry;
                if (expected.getO1().equals(e.getKey().getName()) && expected.getO2().intValue() == e.getValue().intValue()) {
                    correctResultCounter++;
                }
            }
        }
        if (correctResultCounter != expectedResult.size()) {
            fail("results are not complete or correct");
        }
    }

    @Test
    public void SimpleAssignment() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(SimpleAssignment.class.getName());
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(SimpleAssignment.class.getName());
        List<Pair<String, Integer>> expected = new ArrayList<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        checkResultsAtLastStatement(analysis, sparseAnalysis, expected);
    }

    @Test
    public void SimpleAssignment2() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(SimpleAssignment2.class.getName());
        List<Pair<String, Integer>> expected = new ArrayList<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 40));
        checkResultsAtLastStatement(analysis, expected);
    }

    @Test
    public void SimpleAssignment3() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(SimpleAssignment3.class.getName());
        List<Pair<String, Integer>> expected = new ArrayList<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 400));
        checkResultsAtLastStatement(analysis, expected);
    }

    @Test
    public void SimpleAssignment4() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(SimpleAssignment4.class.getName());
        List<Pair<String, Integer>> expected = new ArrayList<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 413));
        checkResultsAtLastStatement(analysis, expected);
    }

    @Test
    public void SimpleAssignment5() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(SimpleAssignment5.class.getName());
        List<Pair<String, Integer>> expected = new ArrayList<>();
        expected.add(new Pair("a", 13));
        expected.add(new Pair("b", 200));
        checkResultsAtLastStatement(analysis, expected);
    }

    @Test
    public void FunctionCall2() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(FunctionCall2.class.getName());
        List<Pair<String, Integer>> expected = new ArrayList<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 101));
        expected.add(new Pair("d", 201));
        checkResultsAtLastStatement(analysis, expected);
    }
}
