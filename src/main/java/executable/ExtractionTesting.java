package executable;

import executable.tests.CruzFilipeLarsenMontesi17;
import executable.tests.LangeTuostoYoshida15;
import executable.tests.LangeTuostoYoshida15Sequential;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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
            "theory", new Command(() -> runTests(CruzFilipeLarsenMontesi17.class), "Run the tests from the original theoretical paper [Cruz-Filipe, Larsen, Montesi @ FoSSaCS 2017]"),
            "lty15", new Command(() -> runTests(LangeTuostoYoshida15.class), "Run the tests from the paper [Lange, Tuosto, Yoshida @ POPL 2015]"),
            "lty15-seq", new Command(() -> runTests(LangeTuostoYoshida15Sequential.class), "Run the tests from the paper [Lange, Tuosto, Yoshida @ POPL 2015] *with parallelization disabled*")
    );

    public static void main(String []args){



    }

    private static void runTests(Object testClass){
        for (Method method : testClass.getClass().getDeclaredMethods()){
            try {
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
