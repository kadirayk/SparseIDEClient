import boomerang.scene.jimple.BoomerangPretransformer;
import heros.solver.Pair;
import solver.JimpleIDESolver;
import soot.*;
import soot.options.Options;
import sparse.JimpleSparseIDESolver;

import java.io.File;
import java.util.Set;

public class Main {

    public static void main(String[] args){
        SetUp setUp = new SetUp();
        setUp.executeStaticAnalysis(args[0]);
        System.out.println(setUp.defaultPropCount);
        setUp.executeSparseStaticAnalysis(args[0]);
        System.out.println(setUp.sparsePropCount);
    }

}
