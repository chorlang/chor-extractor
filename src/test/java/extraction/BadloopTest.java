package extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}