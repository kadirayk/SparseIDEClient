package target.constant;

public class Assignment {

    /**
     * Simple assignment
     */
    public void entryPoint() {
        int a = 100;
        int b = 200;
        int c = 400;
        int d = 121;
    }

    public void entryPoint_a(){
        int a = 100;
        int b = inc(a);
    }

    public int inc(int x){
        return x++;
    }

}
