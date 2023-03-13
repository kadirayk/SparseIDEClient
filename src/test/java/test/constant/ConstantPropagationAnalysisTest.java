package test.constant;

import analysis.IDELinearConstantAnalysisProblem;
import analysis.data.DFF;
import heros.InterproceduralCFG;
import heros.solver.Pair;
import heros.sparse.SparseCFGBuilder;
import org.junit.Test;
import solver.JimpleIDESolver;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transformer;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import sparse.CPAJimpleSparseCFGBuilder;
import sparse.JimpleSparseIDESolver;
import target.constant.*;
import test.base.IDETestSetUp;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
                SparseCFGBuilder sparseCFGBuilder = new CPAJimpleSparseCFGBuilder(false);
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
        if (analysis instanceof JimpleIDESolver) {
            JimpleIDESolver solver = (JimpleIDESolver) analysis;
            res = (Map<DFF, Integer>) solver.resultsAt(m.getActiveBody().getUnits().getLast());
        } else if (analysis instanceof JimpleSparseIDESolver) {
            JimpleSparseIDESolver solver = (JimpleSparseIDESolver) analysis;
            res = (Map<DFF, Integer>) solver.resultsAt(m.getActiveBody().getUnits().getLast());
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

    private String msg(Set<Pair<String, Integer>> actual, Set<Pair<String, Integer>> expected) {
        StringBuilder str = new StringBuilder(System.lineSeparator());
        str.append("actual:").append(System.lineSeparator());
        str.append(actual.stream().map(p -> p.toString()).collect(Collectors.joining("-"))).append(System.lineSeparator());
        str.append("expected:").append(System.lineSeparator());
        str.append(expected.stream().map(p -> p.toString()).collect(Collectors.joining("-"))).append(System.lineSeparator());
        return str.toString();
    }

    @Test
    public void Assignment() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Assignment.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Assignment.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }


    @Test
    public void Assignment2() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Assignment2.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Assignment2.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 40));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Assignment3() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Assignment3.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Assignment3.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 400));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Assignment4() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Assignment4.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Assignment4.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 413));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Assignment5() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Assignment5.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Assignment5.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 13));
        expected.add(new Pair("b", 200));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Assignment6() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Assignment6.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Assignment6.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a#3", 101));
        expected.add(new Pair("b#4", 201));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Assignment7() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Assignment7.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Assignment7.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 110));
        expected.add(new Pair("c", 105));
        expected.add(new Pair("d", 210));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Assignment8() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Assignment8.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Assignment8.class.getName());
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
    public void Branching3() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Branching3.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Branching3.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 0));
        expected.add(new Pair("a#2", IDELinearConstantAnalysisProblem.BOTTOM));
        expected.add(new Pair("c", IDELinearConstantAnalysisProblem.BOTTOM));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Branching4() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Branching4.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Branching4.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 0));
        expected.add(new Pair("a#2", IDELinearConstantAnalysisProblem.BOTTOM));
        expected.add(new Pair("b", 10));
        expected.add(new Pair("c", 10));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Branching5() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Branching5.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Branching5.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 0));
        expected.add(new Pair("a#2", IDELinearConstantAnalysisProblem.BOTTOM));
        expected.add(new Pair("b", 10));
        expected.add(new Pair("c", IDELinearConstantAnalysisProblem.BOTTOM));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Branching6() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Branching6.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Branching6.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 0));
        expected.add(new Pair("a#2", 23));
        expected.add(new Pair("b", 10));
        expected.add(new Pair("c", 26));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Context() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Context.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Context.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 300));
        expected.add(new Pair("d", 400));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Context2() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Context2.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Context2.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 101));
        expected.add(new Pair("d", 201));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Context3() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Context3.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Context3.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 113));
        expected.add(new Pair("d", 55));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Context4() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Context4.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Context4.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 101));
        expected.add(new Pair("d", 201));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }


    @Test
    public void Loop() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Loop.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Loop.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("sum", IDELinearConstantAnalysisProblem.BOTTOM));
        expected.add(new Pair("a", IDELinearConstantAnalysisProblem.BOTTOM));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Loop2() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Loop2.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Loop2.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("sum", IDELinearConstantAnalysisProblem.BOTTOM));
        expected.add(new Pair("a", IDELinearConstantAnalysisProblem.BOTTOM));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Loop3() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Loop3.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Loop3.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("sum", IDELinearConstantAnalysisProblem.BOTTOM));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Loop4() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Loop4.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Loop4.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("sum", IDELinearConstantAnalysisProblem.BOTTOM));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Loop5() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Loop5.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Loop5.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("sum", IDELinearConstantAnalysisProblem.BOTTOM));
        expected.add(new Pair("a", IDELinearConstantAnalysisProblem.BOTTOM));
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

    @Test
    public void Field4() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Field4.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Field4.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("field.x", 101));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Field5() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Field5.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Field5.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("field.x", 100));
        expected.add(new Pair("field.y", 100));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Field6() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Field6.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Field6.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("field.x", 101));
        expected.add(new Pair("alias.x", 101));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void Field7() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(Field7.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(Field7.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("field.x", 100));
        expected.add(new Pair("alias.x", 100));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void NonLinear() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(NonLinear.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(NonLinear.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 1));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

    @Test
    public void NonLinear2() {
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(NonLinear2.class.getName());
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(NonLinear2.class.getName());
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("i", IDELinearConstantAnalysisProblem.BOTTOM));
        expected.add(new Pair("hashCode", IDELinearConstantAnalysisProblem.BOTTOM));
        checkResults(defaultIDEResult, sparseIDEResult, expected);
    }

}
