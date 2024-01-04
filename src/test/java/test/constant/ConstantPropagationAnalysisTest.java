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
import sparse.DefaultSparseCFGBuilder;
import sparse.JimpleSparseIDESolver;
import target.constantbench.*;
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
                SparseCFGBuilder sparseCFGBuilder = new DefaultSparseCFGBuilder(false);
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

    private void checkResults(Set<Pair<String, Integer>> defaultIDEResult, Set<Pair<String, Integer>> sparseIDEResult, Set<Pair<String, Integer>> expected, String target) {
        // first remove intermediate vars
        Supplier<Predicate<Pair<String, Integer>>> pred = () -> p -> !(p.getO1().startsWith("$stack") || p.getO1().startsWith("varReplacer"));
        defaultIDEResult = defaultIDEResult.stream().filter(pred.get()).collect(Collectors.toSet());
        sparseIDEResult = sparseIDEResult.stream().filter(pred.get()).collect(Collectors.toSet());
        assertEquals(defaultIDEResult, sparseIDEResult);
        assertTrue(msg(defaultIDEResult, expected), defaultIDEResult.containsAll(expected));
        assertTrue(msg(sparseIDEResult, expected), sparseIDEResult.containsAll(expected));
        System.out.println("Test case run: " + target);
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
        String name = Assignment.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }


    @Test
    public void Assignment2() {
        String name = Assignment2.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 40));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Assignment3() {
        String name = Assignment3.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 400));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Assignment4() {
        String name = Assignment4.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 413));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Assignment5() {
        String name = Assignment5.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 13));
        expected.add(new Pair("b", 200));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Assignment6() {
        String name = Assignment6.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 101));
        expected.add(new Pair("b", 201));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Assignment7() {
        String name = Assignment7.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 110));
        expected.add(new Pair("c", 105));
        expected.add(new Pair("d", 210));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Assignment8() {
        String name = Assignment8.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 100));
        expected.add(new Pair("c", 100));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Assignment9() {
        String name = Assignment9.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair(name + ".a", 100));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Branching() {
        String name = Branching.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 10));
        expected.add(new Pair("b", 1));
        expected.add(new Pair("c", 14));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Branching2() {
        String name = Branching2.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 0));
        expected.add(new Pair("b", 42));
        expected.add(new Pair("c", 13));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Branching3() {
        String name = Branching3.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", IDELinearConstantAnalysisProblem.BOTTOM));
        expected.add(new Pair("c", IDELinearConstantAnalysisProblem.BOTTOM));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Branching4() {
        String name = Branching4.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", IDELinearConstantAnalysisProblem.BOTTOM));
        expected.add(new Pair("b", 10));
        expected.add(new Pair("c", 10));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Branching5() {
        String name = Branching5.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", IDELinearConstantAnalysisProblem.BOTTOM));
        expected.add(new Pair("b", 10));
        expected.add(new Pair("c", IDELinearConstantAnalysisProblem.BOTTOM));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Branching6() {
        String name = Branching6.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 23));
        expected.add(new Pair("b", 10));
        expected.add(new Pair("c", 26));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Context() {
        String name = Context.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 300));
        expected.add(new Pair("d", 400));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Context2() {
        String name = Context2.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 101));
        expected.add(new Pair("d", 201));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Context3() {
        String name = Context3.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 113));
        expected.add(new Pair("d", 55));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Context4() {
        String name = Context4.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("b", 200));
        expected.add(new Pair("c", 101));
        expected.add(new Pair("d", 201));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Context5() {
        String name = Context5.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("field.x", 100));
        expected.add(new Pair("field.y", 200));
        expected.add(new Pair("b", 100));
        expected.add(new Pair("c", 200));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Context6() {
        String name = Context6.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair(name + ".a", 100));
        expected.add(new Pair("x", 100));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }


    @Test
    public void Loop() {
        String name = Loop.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("sum", IDELinearConstantAnalysisProblem.BOTTOM));
        expected.add(new Pair("a", IDELinearConstantAnalysisProblem.BOTTOM));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Loop2() {
        String name = Loop2.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("sum", IDELinearConstantAnalysisProblem.BOTTOM));
        expected.add(new Pair("a", IDELinearConstantAnalysisProblem.BOTTOM));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Loop3() {
        String name = Loop3.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("sum", IDELinearConstantAnalysisProblem.BOTTOM));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Loop4() {
        String name = Loop4.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("sum", IDELinearConstantAnalysisProblem.BOTTOM));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Loop5() {
        String name = Loop5.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("sum", IDELinearConstantAnalysisProblem.BOTTOM));
        expected.add(new Pair("a", IDELinearConstantAnalysisProblem.BOTTOM));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Field() {
        String name = Field.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("field.x", 100));
        expected.add(new Pair("field.y", 200));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Field2() {
        String name = Field2.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("field.x", 100));
        expected.add(new Pair("a", 100));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Field3() {
        String name = Field3.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("field.x", 100));
        expected.add(new Pair("alias.x", 100));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Field4() {
        String name = Field4.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("field.x", 101));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Field5() {
        String name = Field5.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("field.x", 100));
        expected.add(new Pair("field.y", 100));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Field6() {
        String name = Field6.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("field.x", 101));
        expected.add(new Pair("alias.x", 101));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Field7() {
        String name = Field7.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 100));
        expected.add(new Pair("field.x", 100));
        expected.add(new Pair("alias.x", 100));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void NonLinear() {
        String name = NonLinear.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("a", 1));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void NonLinear2() {
        String name = NonLinear2.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("i", IDELinearConstantAnalysisProblem.BOTTOM));
        expected.add(new Pair("hashCode", IDELinearConstantAnalysisProblem.BOTTOM));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Array() {
        String name = Array.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("A.i_0", 100));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Array2() {
        String name = Array2.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("A.i_0", 100));
        expected.add(new Pair("a", 100));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Array3() {
        String name = Array3.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("A.i_0", 100));
        expected.add(new Pair("A.i_1", 200));
        expected.add(new Pair("A.i_2", 400));
        expected.add(new Pair("B.i_0", 100));
        expected.add(new Pair("B.i_1", 200));
        expected.add(new Pair("B.i_2", 400));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Array4() {
        String name = Array4.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("A.i_0", 100));
        expected.add(new Pair("A.i_1", 200));
        expected.add(new Pair("A.i_2", 400));
        expected.add(new Pair("B.i_0", 100));
        expected.add(new Pair("B.i_1", 200));
        expected.add(new Pair("B.i_2", 400));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }

    @Test
    public void Array5() {
        String name = Array5.class.getName();
        JimpleIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> analysis = executeStaticAnalysis(name);
        Set<Pair<String, Integer>> defaultIDEResult = getResult(analysis);
        JimpleSparseIDESolver<?, ?, ? extends InterproceduralCFG<Unit, SootMethod>> sparseAnalysis = executeSparseStaticAnalysis(name);
        Set<Pair<String, Integer>> sparseIDEResult = getResult(sparseAnalysis);
        Set<Pair<String, Integer>> expected = new HashSet<>();
        expected.add(new Pair("A.i_999", 100));
        expected.add(new Pair("B.i_42", 100));
        expected.add(new Pair("a", 100));
        checkResults(defaultIDEResult, sparseIDEResult, expected, name);
    }


}
