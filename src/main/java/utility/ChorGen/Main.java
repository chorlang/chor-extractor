package utility.ChorGen;

import java.io.IOException;

// pass nothing to generate all tests from TestGenerator
// pass 4 parameters (length, numProcesses, numIfs, numProcedures) to generate one specific test
// pass 5 parameters - 4 previous and mask like (--+-), where "+" is for parameter(s) you want to increase to generate a set of tests
public class Main {
    public static void main(String args[]) throws GeneratorException, IOException {
        System.out.println("pass nothing to generate all tests from TestGenerator");
        System.out.println("pass 4 parameters (length, numProcesses, numIfs, numProcedures) to generate one specific test");
        System.out.println("pass 5 parameters - 4 previous and mask like (--+-), where \"+\" is for parameter(s) you want to increase to generate a set of tests");

        switch (args.length) {
            case 0: {
                TestGenerator.main(args);
                break;
            }
            case 4: {
                TestGenerator t = new TestGenerator();
                t.generateCustomTest(false, args);
                break;
            }
            case 5: {
                TestGenerator t = new TestGenerator();
                t.generateCustomTest(true, args);
                break;
            }
        }
    }
}
