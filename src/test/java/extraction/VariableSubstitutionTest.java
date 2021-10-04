package extraction;

import extraction.network.Network;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import parsing.Parser;

/**
 * Test parameters, and other cases where variables need to be replaced by real values for the network to extract.
 */
public class VariableSubstitutionTest {
    @Test
    void parserParameterTest() {
        String test = "a { def X(a,b,c) { Y(c) } def Y(a) { z() } def Z() { W } def W { stop } main { X(a,b,c) } }";
        Network network = Parser.stringToNetwork(test);
        Assertions.assertNotNull(network);
    }

    @Test
    void basicParameters(){
        String test =   "a { def X(q) { q?; stop } main { b!<msg>; X(b)} } |" +
                        "b { main { a?; a!<resp>; stop } }";
        String expected = "main {a.msg->b; b.resp->a; stop}";
        String actual = Extraction.extractChoreography(test).toString();
        Assertions.assertEquals(expected, actual);
    }
}