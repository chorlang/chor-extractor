package executable.tests;

import extraction.Extraction;
import extraction.Strategy;

import java.util.Set;

public class TestUtil {
    static void printExtractionResult(String network, String choreography, String expected){
        if (!expected.equals(choreography)) {
            System.out.println("ERROR: Extracted choreography do not mach expected varue");
            System.out.println("Expected choreography:\n" + expected);
            System.out.println("Extracted choreography:\n" + choreography);
        }
        else
            printExtractionResult(network, choreography);
    }

    static void printExtractionResult(String network, String choreography){
        System.out.println("input network:\n\t" + network + "\nOutput choreography\n\t" + choreography);
    }

    public static void runExtractionTest(String network, String ... services){
        printExtractionResult(network,
                Extraction.extract(network, Strategy.Default, Set.of(services)).toString());
    }
}
