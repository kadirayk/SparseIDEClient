package target.constant;

public class FunctionCall2 {

    int increment(int a) {
        return a + 1;
    }

    public void entryPoint() {
        int a = 100;
        int b = 200;
        int c = increment(a);
        int d = increment(b);
    }

}
