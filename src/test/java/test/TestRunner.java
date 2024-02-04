package test;

import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eval.EvalHelper;
import eval.Main;
import heros.solver.Pair;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import test.base.IDETestSetUp;
import test.constant.ConstantPropagationAnalysisTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TestRunner {

    private static final String [] callgraph_algorithms = {"CHA", "RTA", "VTA", "SPARK", "insens", "B-2o", "D-2o", "D-2c",
            "1c", "2c", "1o", "2o", "1t", "2t", "1h", "2h" , "1ht", "2ht", "M-1o", "M-2o", "M-1c", "M-2c", "E-1o", "E-2o",
            "T-1o", "T-2o", "Z-1o", "Z-2o", "Z-1c", "Z-2c", "s-1c", "s-2c"};

    private static String output_file_path = "";

    private static String results_file_path = "";

    private static int startingIndex;

    private static int endingIndex;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static void main(String[] args) {
        if(args[0] != null){
            IDETestSetUp.soot_class_path = args[0];
        }
        output_file_path = args[1];
        results_file_path = args[2];
//        deleteFileIfExists(output_file_path);
//        deleteFileIfExists(results_file_path);
        if(args.length > 3 && args[3] != null && args[4] != null){
            startingIndex = Integer.parseInt(args[3]);
            endingIndex = Integer.parseInt(args[4]);
        }
        else{
            startingIndex = 0;
            endingIndex = 40;
        }
        // Check if the file exists
        try {
            new TestRunner().runTests();
        }
        catch (Exception ignored){

        }
    }

    public static void deleteFileIfExists(String filePath){
        try {
            // Check if the file exists
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                // Delete the file
                Files.delete(path);
                System.out.println("File deleted successfully.");
            } else {
                System.out.println("File does not exist.");
            }
        } catch (IOException e) {
            // Handle potential IOException, such as permission issues
            e.printStackTrace();
        }
    }
    public void runTests() throws Exception {
        int i = 0;
        for (String algorithm : callgraph_algorithms) {
            ConstantPropagationAnalysisTest constantPropagationAnalysisTest = new ConstantPropagationAnalysisTest();
            Main.CallgraphAlgorithm cg_algo;
            String qilin_pta = "";
            if(i > 3){
                cg_algo = Main.parseCallgraphAlgorithm("QILIN");
                qilin_pta = algorithm;
            }
            else{
                cg_algo = Main.parseCallgraphAlgorithm(algorithm);
            }
            ConstantPropagationAnalysisTest.cg_algo = cg_algo;
            ConstantPropagationAnalysisTest.qilin_pta = qilin_pta;
            String[] methodNames = getTestMethodNames(constantPropagationAnalysisTest);
            runContextTestMethods(constantPropagationAnalysisTest, methodNames);
//            runTestMethods(constantPropagationAnalysisTest, methodNames);
            i++;
        }
    }

    private static void appendObjectToFile(String filePath, ErrorInfo errorInfo) {
        try {
            // Read the existing content from the file
            List<ErrorInfo> existingList = readFromFile(filePath);

            // Append the new object to the list
            existingList.add(errorInfo);

            // Write the updated list back to the file
            writeToFile(filePath, existingList);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<ErrorInfo> readFromFile(String filePath) throws IOException {
        try {
            File file = new File(filePath);

            if (!file.exists() || file.length() == 0) {
                // If the file is empty or doesn't exist, return an empty list
                return new ArrayList<>();
            } else {
                // Read the existing content from the file
                return objectMapper.readValue(file, new TypeReference<List<ErrorInfo>>() {});
            }
        } catch (JsonEOFException e) {
            // Treat JsonEOFException as an indication of an empty array
            System.out.println("Warning: JSON array is incomplete or missing closing bracket. Treating it as an empty array.");
            return new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static void writeToFile(String filePath, List<ErrorInfo> list) {
        try {
            // Use try-with-resources to automatically close the FileWriter
            try (java.io.Writer writer = new java.io.FileWriter(filePath)) {
                objectMapper.writeValue(writer, list);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runTestMethods(ConstantPropagationAnalysisTest constantPropagationAnalysisTest, String[] methodNames) {
        for (String methodName : methodNames) {
            // Run each test method individually
            ConstantPropagationAnalysisTest.currentMethodName = methodName;
            Result result = runTestMethod(constantPropagationAnalysisTest, methodName);

            Main.CallgraphAlgorithm callgraphAlgorithm = EvalHelper.getCallgraphAlgorithm();
            String qilinPta = EvalHelper.getQilin_PTA();
            String cg_algo = callgraphAlgorithm.toString().contains("QILIN") ? qilinPta : callgraphAlgorithm.toString();
            writeCsvResults(methodName, cg_algo, result.wasSuccessful() ? "success" : "failure");
            if (!result.wasSuccessful()) {
                System.out.println("Test failure for " + methodName + " with " + cg_algo);
                appendObjectToFile(output_file_path, new ErrorInfo(methodName, result.getFailures().toString(), cg_algo));
            }
        }
    }

    private void runContextTestMethods(ConstantPropagationAnalysisTest constantPropagationAnalysisTest, String[] methodNames){
        for (String methodName : methodNames) {
            // Run each test method individually
            ConstantPropagationAnalysisTest.currentMethodName = methodName;
            Result result = runTestMethod(constantPropagationAnalysisTest, "checkCallGraphs");

            Main.CallgraphAlgorithm callgraphAlgorithm = EvalHelper.getCallgraphAlgorithm();
            String qilinPta = EvalHelper.getQilin_PTA();
            String cg_algo = callgraphAlgorithm.toString().contains("QILIN") ? qilinPta : callgraphAlgorithm.toString();
            writeCsvResults(methodName, cg_algo, result.wasSuccessful() ? "success" : "failure");
            if (!result.wasSuccessful()) {
                System.out.println("Test failure for " + methodName + " with " + cg_algo);
                appendObjectToFile(output_file_path, new ErrorInfo(methodName, result.getFailures().toString(), cg_algo));
            }
        }
    }

    private static void writeCsvResults(String methodName,String cg_algo,  String isSuccess){
        File file = new File(results_file_path);
        if(!file.exists()) {
            try (FileWriter writer = new FileWriter(file)) {
                String str = "methodName" +
                        "," +
                        "cg_algo" +
                        "," +
                        "result" +
                        System.lineSeparator();
                writer.write(str);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try (FileWriter writer = new FileWriter(file, true)) {
            String str = methodName +
                    "," +
                    cg_algo +
                    "," +
                    isSuccess +
                    System.lineSeparator();
            writer.write(str);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Result runTestMethod(Object testInstance, String methodName) {
        Request request = Request.method(testInstance.getClass(), methodName);
        return new JUnitCore().run(request);
    }

    private static String[] getTestMethodNames(Object testInstance) {
        String[] array = Arrays.stream(testInstance.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Test.class)
                        && method.getReturnType().equals(void.class))
                .map(Method::getName)
                .filter(methodName -> methodName.contains("Context"))
                .toArray(String[]::new);
        return array;
//        return Arrays.copyOfRange(array, startingIndex, endingIndex);
    }

    public static class ErrorInfo {

        public ErrorInfo(String testName, String expectedResult, String cg_algo){
        }

    }
}
