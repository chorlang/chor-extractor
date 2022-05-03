package utility.ChorGen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/*
 * Here we generate the tests we want.
 *
 * Cheat sheet:
 * ChoreographyGenerator(int length, int numProcesses, int numIfs, int numProcedures)
 *
 * Statistical info:
 * with n procs, the chance that consecutive communications are independent is (n-2)(n-3)/(n(n-1))
 * 4 <-> 1/6
 * 5 <-> 30%
 * 6 <-> 2/5
 * 7 <-> 10/21
 *
 * Comments:
 * - When procedures are involved, we may get dead code; this implies that e.g. the number of ifs
 *   may be smaller than expected. I added some static analysis to eliminate this situation and only
 *   generate tests with the required parameters.
 * - We're currently not amending the choreographies. Amending will increase the size, but this seems
 *   unavoidable.
 */
public class TestGenerator {

    private static final int NUMBER_OF_TESTS = 10;
    private static int generatedTests = 0,
            badTests = 0;
    private static final String LOG_FILE = "summary.log";
    private static final String TEST_DIR = "GeneratedTests/";

    private static void makeALotOfTests(int length, int numProcesses, int numIfs, int numProcedures, int numSpawns, BufferedWriter logFile)
            throws IOException, GeneratorException {

        // standard filename
        String testFileName = "choreography-" + length + "-" + numProcesses + "-" + numIfs + "-" + numProcedures;
        logFile.write("Generating file " + testFileName);
        logFile.newLine();

        // seed info
        ChoreographyGenerator tester = new ChoreographyGenerator(length, numProcesses, numIfs, numProcedures, numSpawns);
        logFile.write("Seed (for reproducibility): " + tester.getSeed());
        logFile.newLine();

        // actual generation of choreographies
        BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_DIR + testFileName));
        for (int j = 0; j < NUMBER_OF_TESTS; j++) {
            Choreography c = tester.generate();
            generatedTests++;
            while (c.hasDeadCode()) {
                c = tester.generate();
                generatedTests++;
                badTests++;
            }
            writer.write("*** C" + generatedTests + " ***");
            writer.newLine();
            writer.write(c.amend().toString());
            writer.newLine();
        }
        writer.close();
    }

    /*
     * This will be refactored at a later stage.
     */
    private static void makeALotOfTestsWithSeed(long seed, int length, int numProcesses, int numIfs, int numProcedures, int numSpawns, BufferedWriter logFile)
            throws IOException, GeneratorException {

        // standard filename
        String testFileName = "choreography-" + length + "-" + numProcesses + "-" + numIfs + "-" + numProcedures + "-" + numSpawns;
        logFile.write("Generating file " + testFileName);
        logFile.newLine();

        // seed info
        ChoreographyGenerator tester = new ChoreographyGenerator(seed, length, numProcesses, numIfs, numProcedures, numSpawns);
        logFile.write("Seed (for reproducibility): " + tester.getSeed());
        logFile.newLine();

        // actual generation of choreographies

        String path = TEST_DIR;
        File f = new File(path);
        if (!f.exists()) f.mkdir();

        BufferedWriter writer = new BufferedWriter(new FileWriter(path + testFileName));
        for (int j = 0; j < NUMBER_OF_TESTS; j++) {
            Choreography c = tester.generate();
            generatedTests++;
            while (c.hasDeadCode()) {
                c = tester.generate();
                generatedTests++;
                badTests++;
            }
            writer.write("*** C" + generatedTests + " ***");
            writer.newLine();
            writer.write(c.amend().toString());
            writer.newLine();
        }
        writer.close();
    }

    /*
     * Pretty-printing of headers & stuff
     */
    private static void niceWrite(BufferedWriter file, String message) throws IOException {
        file.write("+");
        for (int i = 0; i < message.length() + 2; i++)
            file.write("-");
        file.write("+");
        file.newLine();
        file.write("| " + message + " |");
        file.newLine();
        file.write("+");
        for (int i = 0; i < message.length() + 2; i++)
            file.write("-");
        file.newLine();
    }

    public static void main(String[] args) throws IOException, GeneratorException {
//         ChoreographyGenerator tester = new ChoreographyGenerator(1031045458752510205L,10,5,3,3);
//         Choreography c = tester.generate();
//         while (c.hasDeadCode())
//             c = tester.generate();
//         c = tester.generate();
//         while (c.hasDeadCode())
//             c = tester.generate();
//         System.out.println("SEED: " + tester.getSeed());
//         System.out.println(c.toString()+"\n");
//         System.out.println(c.amend().toString()+"\n");
//         System.exit(0);

//	BufferedWriter logFile = new BufferedWriter(new FileWriter(LOG_FILE));
//
//	niceWrite(logFile,"STARTING GENERATION...");
//	logFile.newLine();
//
//	makeALotOfTestsWithSeed(-9129708236714512406L,10,5,3,3,logFile);
//	makeALotOfTestsWithSeed(8463895940458588614L,10,6,0,0,logFile);
//	makeALotOfTestsWithSeed(-6338169410111561988L,10,5,0,2,logFile);
//
//        niceWrite(logFile,"Generated "+generatedTests+" tests, of which "+badTests+" contain dead code.");
//	logFile.close();
//
//	System.exit(0);

        BufferedWriter logFile = new BufferedWriter(new FileWriter(LOG_FILE));

        niceWrite(logFile, "STARTING GENERATION...");
        logFile.newLine();
/*
//        niceWrite(logFile, "Test 1: communications only, increasing lengths");
        for (int i = 50; i <= 2100; i += 50)
            makeALotOfTestsWithSeed(0L, i, 6, 0, 0, logFile);
//        logFile.newLine();
//
//        niceWrite(logFile, "Test 2: communications and ifs, fixed length, increasing number of ifs");
        for (int i = 10; i <= 40; i += 10)
            makeALotOfTestsWithSeed(0L, 50, 6, i, 0, logFile);
//        logFile.newLine();
//
//        niceWrite(logFile, "Test 3: inserting recursion; two varying parameters - #ifs and #procs");
        for (int i = 0; i <= 5; i++)
            for (int j = 0; j <= 15; j += 5)
                makeALotOfTestsWithSeed(0L, 200, 5, i, j, logFile);
//        logFile.newLine();
//
//        niceWrite(logFile, "Test 4: communications only, fixed length, increasing number of processesInChoreography");
        for (int i = 5; i <= 100; i += 5)
            makeALotOfTestsWithSeed(0L, 500, i, 0, 0, logFile);
//        logFile.newLine();
//
*/

//        niceWrite(logFile, "Test 5: inserting recursion; one varying parameter - #ifs");
//        for (int i = 1; i <= 10; i++)
//            makeALotOfTestsWithSeed(0L, 100, 50, i, 5, logFile);
//        logFile.newLine();

//        niceWrite(logFile, "Test 6: 10 ifs, increasing procedures");
        for (int i = 1; i <= 3; i++)
            makeALotOfTestsWithSeed(0L, 20, 5, 8, i, 2, logFile);
//        logFile.newLine();

        niceWrite(logFile, "Generated " + generatedTests + " tests, of which " + badTests + " contain dead code.");
        logFile.close();
    }

    public void generateCustomTest(Boolean it, String... args) throws IOException, GeneratorException {
        BufferedWriter logFile = new BufferedWriter(new FileWriter(LOG_FILE));

        niceWrite(logFile, "STARTING GENERATION...");
        logFile.newLine();

        int len = Integer.parseInt(args[0]);
        int pr = Integer.parseInt(args[1]);
        int cond = Integer.parseInt(args[2]);
        int proc = Integer.parseInt(args[3]);

        if (it) {
            String five = args[4];
            len = five.charAt(0) == '-'? 40 : 10;
            pr = five.charAt(1) == '-'? 6 : 10;
            int pr_top = five.charAt(1) == '-'? 6 : 100;
            cond = five.charAt(2) == '-'? 0 : 5;
            proc = five.charAt(3) == '-'? 0 : 5;

            for (int i = len; i <= 40; i += 10)
                for (int j = pr; j <= pr_top; j += 10)
                    for (int k = 0; k <= cond; k ++)
                        for (int l = 0; l <= proc; l++)
                            //int length, int numProcesses, int numIfs, int numProcedures
                            makeALotOfTestsWithSeed(0L, i, j, k, l, 0, logFile);

        } else
            makeALotOfTestsWithSeed(0L, len, pr, cond, proc, 0, logFile);
    }
}
