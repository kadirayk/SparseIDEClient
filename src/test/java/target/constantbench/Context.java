package target.constantbench;

public class Context {

    int id(int a) {
        return a;
    }

    public void entryPoint() {
        int a = 100;
        int b = 200;
        int c = id(300);
        int d = id(400);
    }

}
