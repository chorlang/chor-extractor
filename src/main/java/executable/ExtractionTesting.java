package executable;

import executable.tests.Benchmarks;
import executable.tests.CruzFilipeLarsenMontesi17;
import executable.tests.LangeTuostoYoshida15;
import executable.tests.LangeTuostoYoshida15Sequential;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Scanner;

public class ExtractionTesting {
    private static record Command(Runnable runnable, String description) { }


    private static final Map<String, Command> commands = Map.of(
            "help", new Command(ExtractionTesting::printHelp, "Prints this help information"),
            "theory", new Command(() -> runTests(new CruzFilipeLarsenMontesi17()), "Run the tests from the original theoretical paper [Cruz-Filipe, Larsen, Montesi @ FoSSaCS 2017]"),
            "lty15", new Command(() -> runTests(new LangeTuostoYoshida15()), "Run the tests from the paper [Lange, Tuosto, Yoshida @ POPL 2015]"),
            "lty15-seq", new Command(() -> runTests(new LangeTuostoYoshida15Sequential()), "Run the tests from the paper [Lange, Tuosto, Yoshida @ POPL 2015] *with parallelization disabled*"),
            "benchmark", new Command(ExtractionTesting::runBenchmarks, "Run the extraction benchmarking suite"),
            "bisimcheck", new Command(ExtractionTesting::runBisimilarity, "Check that the choreographies extracted in the benchmark are correct, i.e., they are bisimilar to the respective originals"),
            "exit", new Command(() -> System.out.println("Goodbye"), "Closes the application")
    );

    public static void main(String []args){
        if (args.length == 0) {
            printHelp();
            CMDInterface();
        }
        else{
            Command toExecute = commands.get(args[0]);
            if (toExecute == null){
                System.out.println("Could not recognize argument " + args[0]);
                printHelp();
                System.exit(1);
            }
            toExecute.runnable.run();
        }
    }

    private static void CMDInterface(){
        var inputReader = new Scanner(System.in);
        String command;
        do {
            System.out.println("Enter command: ");
            command = inputReader.next().toLowerCase();
            Command toExecute = commands.get(command);
            if (toExecute == null){
                System.out.println("Could not recognize command");
                printHelp();
            }
            else{
                toExecute.runnable.run();
            }
        } while (!command.equals("exit"));
    }

    private static void runTests(Object testClass){
        for (Method method : testClass.getClass().getDeclaredMethods()){
            try {
                var inst = testClass.getClass();
                var startTime = System.currentTimeMillis();
                System.out.println("Running " + method.getName());
                method.invoke(testClass);
                System.out.println("Elapsed time: " + (System.currentTimeMillis() - startTime) + "ms\n");
            } catch (IllegalAccessException | InvocationTargetException e){
                System.out.println("ERROR: Could not invoke method in class " + testClass.getClass().getName());
                e.printStackTrace();
            }
        }
    }

    private static void printHelp() {
        System.out.println("List of available commands (<name of command>\t<description>)");
        commands.forEach((key, value) -> System.out.println("\t" + key + "\t\t" + value.description));
    }

    private static void runBenchmarks(){
        System.out.println("=== Extracting all test networks from directory tests ===\n");
        Benchmarks.INSTANCE.extractionTest();
    }

    private static void runBisimilarity(){
        System.out.println("=== Checking that all extracted choreographies are correct, using bisimilarity ===\n");
        Benchmarks.INSTANCE.extractionSoundness();
    }
}
