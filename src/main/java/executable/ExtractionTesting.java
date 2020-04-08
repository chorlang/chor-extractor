package executable;

import executable.tests.CruzFilipeLarsenMontesi17;
import executable.tests.LangeTuostoYoshida15;
import executable.tests.LangeTuostoYoshida15Sequential;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ExtractionTesting {
    private static class Command{
        Runnable runnable;
        String description;
        public Command(Runnable runnable, String description){
            this.runnable = runnable;
            this.description = description;
        }
    }

    private static Map<String, Command> commands = Map.of(
            "help", new Command(ExtractionTesting::printHelp, "Prints this help information"),
            "theory", new Command(() -> runTests(new CruzFilipeLarsenMontesi17()), "Run the tests from the original theoretical paper [Cruz-Filipe, Larsen, Montesi @ FoSSaCS 2017]"),
            "lty15", new Command(() -> runTests(new LangeTuostoYoshida15()), "Run the tests from the paper [Lange, Tuosto, Yoshida @ POPL 2015]"),
            "lty15-seq", new Command(() -> runTests(new LangeTuostoYoshida15Sequential()), "Run the tests from the paper [Lange, Tuosto, Yoshida @ POPL 2015] *with parallelization disabled*")
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
            command = inputReader.next();
            Command toExecute = commands.get(command);
            if (toExecute == null){
                System.out.println("Could not recognize command");
                printHelp();
                System.out.println("\texit\t\tCloses the application");
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
}
