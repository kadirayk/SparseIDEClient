package target.constant;

public class Field3 {

    int x;
    int y;

    public void entryPoint() {
        Field3 field = new Field3();
        Field3 alias = field;
        field.x = 100;
    }

}
