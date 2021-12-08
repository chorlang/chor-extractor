package extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BadloopTest{

    @Test
    void tst2p(){
        var test =
                "a1 {def X {b1!<e>; b1?; X} main {X}} |" +
                        "b1 {def X {a1?; a1!<e>; X} main {X}} |" +
                        "a2 {def X {b2!<e>; b2?; X} main {X}} |" +
                        "b2 {def X {a2?; a2!<e>; X} main {X}}";

        var actual = Extraction.extractChoreography(test).toString();
        var expected = "def X1 { a1.e->b1; b1.e->a1; X1 } main {X1} || def X1 { a2.e->b2; b2.e->a2; X1 } main {X1}";

        assertEquals(expected, actual);
    }

    @Test
    void tst3p(){
        var test =
                "a1 {def X {b1!<e>; b1?; X} main {X}} |" +
                        "b1 {def X {a1?; a1!<e>; X} main {X}} |" +
                        "a2 {def X {b2!<e>; b2?; X} main {X}} |" +
                        "b2 {def X {a2?; a2!<e>; X} main {X}} |" +
                        "a3 {def X {b3!<e>; b3?; X} main {X}} |" +
                        "b3 {def X {a3?; a3!<e>; X} main {X}}";
        var actual = Extraction.extractChoreography(test).toString();
        var expected = "def X1 { a1.e->b1; b1.e->a1; X1 } main {X1} || def X1 { a2.e->b2; b2.e->a2; X1 } main {X1} || def X1 { a3.e->b3; b3.e->a3; X1 } main {X1}";

        assertEquals(expected, actual);
    }

    @Test
    void tst4p(){
        var test =
                "a1 {def X {b1!<e>; b1?; X} main {X}} |" +
                        "b1 {def X {a1?; a1!<e>; X} main {X}} |" +
                        "a2 {def X {b2!<e>; b2?; X} main {X}} |" +
                        "b2 {def X {a2?; a2!<e>; X} main {X}} |" +
                        "a3 {def X {b3!<e>; b3?; X} main {X}} |" +
                        "b3 {def X {a3?; a3!<e>; X} main {X}} |" +
                        "a4 {def X {b4!<e>; b4?; X} main {X}} |" +
                        "b4 {def X {a4?; a4!<e>; X} main {X}}";
        var actual = Extraction.extractChoreography(test).toString();
        var expected = "def X1 { a1.e->b1; b1.e->a1; X1 } main {X1} || def X1 { a2.e->b2; b2.e->a2; X1 } main {X1} || def X1 { a3.e->b3; b3.e->a3; X1 } main {X1} || def X1 { a4.e->b4; b4.e->a4; X1 } main {X1}";

        assertEquals(expected, actual);
    }

    //The following two test intentionally fail extraction, but ideally they would succeed

    //Tests that when a then branch of a conditional creates a node, but the else branch
    //creates a bad loop, that the algorithm backtracks correctly.
    @Test
    void thenNodeElseBadloop(){
        String network =
                "a {def X { b?; if e then b+KO; c!<hi>; stop else b+OK; X } main {X} } |" +
                "b {def X { a!<e>; a&{KO: stop, OK: X} } main {X} } |" +
                "c {main {a?; stop} }";
        //This test is supposed to cover specific parts of the extraction algorithm, so the
        //strategy is specified
        var actual = Extraction.extractChoreography(network, Strategy.InteractionsFirst);

        assertNull(actual.choreographies.get(0), "A choreography was extracted from an non-extractable network");

        var statistics = actual.statistics.get(0);
        Assertions.assertTrue(statistics.badLoopCount == 1 && statistics.nodeCount == 1, "The extraction does not appear to have gone though the steps expected of this test. Have major changes been made, or did the test fail?");
    }

    //Tests that when a then branch of a conditional creates a loop, but the else branch
    //is non-extractable, that the then loop is removed.
    //God, it is difficult to create exactly the right conditions.
    /*@Test
    void thenLoopElseFail(){
        String network =
                "a {def X { b?; if e then X else b+OK; X } main {spawn s with stop continue X} } |" +
                "b {def X { a!<e>; X } main {X} }";
        //This test is supposed to cover specific parts of the extraction algorithm, so the
        //strategy is specified
        var actual = Extraction.extractChoreography(network, Strategy.InteractionsFirst);

        assertNull(actual.choreographies.get(0), "A choreography was extracted from an non-extractable network");

        var statistics = actual.statistics.get(0);
        Assertions.assertTrue(statistics.badLoopCount == 1 && statistics.nodeCount == 1, "The extraction does not appear to have gone though the steps expected of this test. Have major changes been made, or did the test fail?");
    }*/
}