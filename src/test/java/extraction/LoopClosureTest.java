package extraction;

import extraction.choreography.Choreography;
import extraction.network.Network;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import parsing.Parser;

import java.time.Duration;

public class LoopClosureTest {
    @Test //Ensure that resource leaks are detected, to prevent generating infinite SEGs
    void ResourceLeakDetection(){
        String network = "a { def X{ spawn q with X continue X } main { X } }";

        //This allows to extract in a separate thread, and abort preemptively
        final Choreography[] actual = new Choreography[1];
        actual[0] = new Choreography(null, null);//Assign a non-null chor
        Executable extracting = () -> actual[0] = Extraction.extractChoreography(network).choreographies.get(0);

        //If extraction takes very long, an infinite SEG is being generated
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5), extracting, "A resource leak was not detected.");
        //Networks with resource leaks are non-extractable by design
        Assertions.assertNull(actual[0]);
    }

    @Test //Makes detectResourceLeak return false on same sized networks. Still a resource leak later though.
    void ResourceLeakDetection2(){
        String network =
                "p { def L1(q){ q!<hi>; q?; L1(q) }" +
                    "def L2(a,b){ a?; b?; b!<ok>; a!<ok>; L2(a,b) }" +
                    "def X{ spawn a with p?q; L1(q) continue spawn b with p?q; L1(q) continue spawn c with p?a; p?b; L2(a,b) continue a<->c; b<->c; X } main { X } }";

        //This allows to extract in a separate thread, and abort preemptively
        final Choreography[] actual = new Choreography[1];
        actual[0] = new Choreography(null, null);//Assign a non-null chor
        Executable extracting = () -> actual[0] = Extraction.extractChoreography(network).choreographies.get(0);

        //If extraction takes very long, an infinite SEG is being generated
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5), extracting, "A resource leak was not detected.");
        //Networks with resource leaks are non-extractable by design
        Assertions.assertNull(actual[0]);
    }

}
