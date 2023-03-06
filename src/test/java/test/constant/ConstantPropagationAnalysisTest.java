package test.constant;

import analysis.IDELinearConstantAnalysisProblem;
import analysis.data.DFF;
import heros.InterproceduralCFG;
import heros.solver.Pair;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import heros.sparse.SparseCFGBuilder;
import org.junit.Test;
import solver.JimpleIDESolver;
import soot.*;

import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import sparse.CPAJimpleSparseCFGBuilder;
import sparse.JimpleSparseIDESolver;
import target.constant.*;
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
            Pair<String, Integer> pair = new Pair<>(e.getKey().toString(), e.getValue());
            result.add(pair);
        }
        return result;
    }

    private void checkResults(Set<Pair<String, Integer>> defaultIDEResult, Set<Pair<String, Integer>> sparseIDEResult, Set<Pair<String, Integer>> expected) {
        // first remove intermediate vars
        Supplier<Predicate<Pair<String, Integer>>> pred = () -> p -> !(p.getO1().startsWith("$stack") || p.getO1().startsWith("varReplacer"));
        defaultIDEResult = defaultIDEResult.stream().filter(pred.get()).collect(Collectors.toSet());
        sparseIDEResult = sparseIDEResult.stream().filter(pred.get()).collect(Collectors.toSet());
        assertEquals(defaultIDEResult, sparseIDEResult);
        assertTrue(msg(defaultIDEResult, expected), defaultIDEResult.containsAll(expected));
        assertTrue(msg(sparseIDEResult, expected), sparseIDEResult.containsAll(expected));
    }

    private String msg(Set<Pair<String, Integer>> actual, Set<Pair<String, Integer>> expected){
        StringBuilder str = new StringBuilder(System.lineSeparator());
        str.append("actual:").append(System.lineSeparator());
        str.append(actual.stream().map(p->p.toString()).collect(Collectors.joining("-"))).append(System.lineSeparator());
        str.append("expected:").append(System.lineSeparator());
        str.append(expected.stream().map(p->p.toString()).collect(Collectors.joining("-"))).append(System.lineSeparator());
        return str.toString();
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
    public void SimpleAssignment6() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(SimpleAssignment6.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(SimpleAssignment6.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a#3", 101));
        expected.add(new Pair("b#4", 201));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void SimpleAssignment7() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(SimpleAssignment7.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(SimpleAssignment7.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 110));
        expected.add(new Pair("c", 105));
        expected.add(new Pair("d", 210));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void SimpleAssignment8() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(SimpleAssignment8.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(SimpleAssignment8.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 100));
        expected.add(new Pair("c", 100));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Branching() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Branching.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Branching.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 0));
        expected.add(new Pair("a#2", 10));
        expected.add(new Pair("b", 1));
        expected.add(new Pair("c", 14));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Branching2() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Branching2.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Branching2.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 0));
        expected.add(new Pair("b", 0));
        expected.add(new Pair("b#2", 42));
        expected.add(new Pair("c", 13));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void FunctionCall() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(FunctionCall.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(FunctionCall.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 300));
        expected.add(new Pair("d", 400));
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

    @Test
    public void FunctionCall3() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(FunctionCall3.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(FunctionCall3.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 113));
        expected.add(new Pair("d", 55));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Loop() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Loop.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Loop.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("sum", Integer.MAX_VALUE));
        expected.add(new Pair("a", Integer.MAX_VALUE));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Loop2() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Loop2.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Loop2.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("sum", Integer.MAX_VALUE));
        expected.add(new Pair("a", Integer.MAX_VALUE));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Field() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Field.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Field.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("field.x", 100));
        expected.add(new Pair("field.y", 200));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Field2() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Field2.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Field2.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("field.x", 100));
        expected.add(new Pair("a", 100));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Field3() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Field3.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Field3.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("field.x", 100));
        expected.add(new Pair("alias.x", 100));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }
}
