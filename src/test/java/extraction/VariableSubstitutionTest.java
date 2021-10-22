package extraction;

import extraction.network.Network;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import parsing.Parser;

import java.util.Set;

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

    @Test
    void nestedParameters(){
        String test =   "a { def X(p) { p!<msg>; Y(p) } def Y(q) { q?; q!<hi>; Z(q) } def Z(r) { r!<ok>; stop } main { b?; X(b) } } |" +
                        "b { def X(p) { p?; Y(p) } def Y(q) { q!<greet>; q?; Z(q) } def Z(r) { r?; stop } main { a!<start>; X(a) } }";
        String expected = "main {b.start->a; a.msg->b; b.greet->a; a.hi->b; a.ok->b; stop}";
        String actual = Extraction.extractChoreography(test).toString();
        Assertions.assertEquals(expected,actual);
    }

    @Test
    void nestedLoop(){
        String test =   "a { def X(p) { p!<msg>; Y(p) } def Y(q) { q?; q!<hi>; Z(q) } def Z(r) { r!<ok>; X(r) } main { b?; X(b) } } |" +
                        "b { def X(p) { p?; Y(p) } def Y(q) { q!<greet>; q?; Z(q) } def Z(r) { r?; X(r) } main { a!<start>; X(a) } }";
        String expected = "def X1 { a.msg->b; b.greet->a; a.hi->b; a.ok->b; X1 } main {b.start->a; X1}";
        String actual = Extraction.extractChoreography(test).toString();
        Assertions.assertEquals(expected,actual);
    }

    @Test
    void introductions(){
        String test =
                "a { def X(q) { q<->b; stop } main { spawn z with a?d; d?; stop continue X(z) } } | " +
                "b { main { a?c; c!<hi>; stop } }";
        String expected = "main {a spawns a/z0; a.a/z0<->b; b.hi->a/z0; stop}";
        //Spawn not supported by splitter at writing of test
        String actual = new Extraction(Strategy.Default).extractChoreographySequentially(test, Set.of()).toString();
        Assertions.assertEquals(expected, actual);
    }
}