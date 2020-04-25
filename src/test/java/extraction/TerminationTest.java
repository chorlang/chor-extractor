package extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class TerminationTest {

    @Test
    void tst1(){
        var test = "p {main{stop}} | q {main{stop}}";
        var actual = Extraction.extractChoreography(test).toString();
        var expected = "main {stop}";

        assertEquals(expected, actual);
    }

    @Test
    void tst2(){
        var test = "p {main{stop}} | q {def X {stop} main{X}}";
        var actual = Extraction.extractChoreography(test).toString();
        var expected = "main {stop}";

        assertEquals(expected, actual);
    }
}
