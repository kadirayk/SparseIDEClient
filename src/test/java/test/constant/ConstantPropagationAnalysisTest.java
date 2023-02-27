package test.constant;

import analysis.IDELinearConstantAnalysisProblem;
import analysis.data.DFF;
import fj.P;
import heros.InterproceduralCFG;
import heros.solver.Pair;

import java.util.*;
import java.util.stream.Collectors;

import heros.sparse.SparseCFGBuilder;
import org.junit.Test;
import solver.JimpleIDESolver;
import soot.*;

import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import sparse.CPAJimpleSparseCFGBuilder;
import sparse.JimpleSparseIDESolver;
import target.constant.FunctionCall2;
import target.constant.SimpleAssignment;
import target.constant.SimpleAssignment2;
import target.constant.SimpleAssignment3;
import target.constant.SimpleAssignment4;
import target.constant.SimpleAssignment5;
import test.base.IDETestSetUp;

import static org.junit.Assert.*;

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
                SparseCFGBuilder sparseCFGBuilder = new CPAJimpleSparseCFGBuilder(true);
                @SuppressWarnings({"rawtypes", "unchecked"})
                JimpleSparseIDESolver<?, ?, ?> solver = new JimpleSparseIDESolver<>(problem, sparseCFGBuilder);
                solver.solve();
                IDETestSetUp.sparseSolver = solver;
            }
        };
    }

    private Set<Pair<String, Integer>> getResult(Object analysis) {
        SootMethod m = getEntryPointMethod();
        Map<DFF, Integer> res = null;
        Set<Pair<String, Integer>> result = new HashSet<>();
        if(analysis instanceof JimpleIDESolver){
            JimpleIDESolver solver = (JimpleIDESolver) analysis;
            res = (Map<DFF, Integer>) solver.resultsAt(m.getActiveBody().getUnits().getLast());
        }else if(analysis instanceof JimpleSparseIDESolver){
            JimpleSparseIDESolver solver = (JimpleSparseIDESolver) analysis;
            res = (Map<DFF, Integer>)  solver.resultsAt(m.getActiveBody().getUnits().getLast());
        }
        for (Map.Entry<DFF, Integer> e : res.entrySet()) {
            Pair<String, Integer> pair = new Pair<>(e.getKey().getValue().toString(), e.getValue());
            result.add(pair);
        }
        return result;
    }

    @Test
    public void SimpleAssignment() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(SimpleAssignment.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(SimpleAssignment.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    private void checkResults(Set<Pair<String, Integer>> defaultIDEResult, Set<Pair<String, Integer>> sparseIDEResult, Set<Pair<String, Integer>> expected) {
        // first remove intermediate vars
        defaultIDEResult = defaultIDEResult.stream().filter(p -> !p.getO1().startsWith("$stack")).collect(Collectors.toSet());
        sparseIDEResult = sparseIDEResult.stream().filter(p -> !p.getO1().startsWith("$stack")).collect(Collectors.toSet());
        assertEquals(expected, defaultIDEResult);
        assertEquals(expected, sparseIDEResult);
    }

    @Test
    public void SimpleAssignment2() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(SimpleAssignment2.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(SimpleAssignment2.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 40));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void SimpleAssignment3() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(SimpleAssignment3.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(SimpleAssignment3.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 400));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void SimpleAssignment4() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(SimpleAssignment4.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(SimpleAssignment4.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 413));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void SimpleAssignment5() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(SimpleAssignment5.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(SimpleAssignment5.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 13));
        expected.add(new Pair("b", 200));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void FunctionCall2() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(FunctionCall2.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(FunctionCall2.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 101));
        expected.add(new Pair("d", 201));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }
}
