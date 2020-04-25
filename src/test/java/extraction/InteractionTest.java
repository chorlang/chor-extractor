package extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class InteractionTest{

    /* simple interaction */
    @Test
    void tst1(){
        var test = "p { main {q!<e>; q?; stop}} " +
                "| q { main {p?; p!<u>; stop}} " +
                "| r { main {stop}}";
        var actual = Extraction.extractChoreography(test).toString();
        var expected = "main {p.e->q; q.u->p; stop}";

        assertEquals(expected, actual);
    }

    /* interaction with name */
    @Test
    void tst2(){
        var test = "p { def X {q!<e>; stop} main {X}} " +
                "| q { def X {p?; stop} main {X}} " +
                "| r { main {stop}}";
        var actual = Extraction.extractChoreography(test).toString();
        var expected = "main {p.e->q; stop}";

        assertEquals(expected, actual);
    }

    /* interaction with name on the one of the processesInChoreography */
    @Test
    void tst3(){
        var test = "p {main{q?;stop}} | q { def X {p!<e>;stop} main{X}}";
        var actual = Extraction.extractChoreography(test).toString();
        var expected = "main {q.e->p; stop}";

        assertEquals(expected, actual);
    }

    /* interaction with recursive name */
    @Test
    void tst4(){
        var test = "p {def X {q!<e>;X} main {X}} " +
                "| q {def Y{p?; Y} main {Y}} " +
                "| r { main {stop}}";
        var actual = Extraction.extractChoreography(test).toString();
        var expected = "def X1 { p.e->q; X1 } main {X1}";

        assertEquals(expected, actual);
    }

    // parallel execution is switched off now
    /*@Test
    void tst5(){
        var test =
                "p {def X {q!<e>;X} main {X}} | q {def Y{p?; Y} main {Y}} | r { main {stop}} || " +
                "p {def X {q!<e>;X} main {X}} | q {def Y{p?; Y} main {Y}} | r { main {stop}}";

        var args = arrayListOf("-c", test);

        var actual = Extraction.main(args).toString();
        var expected = "def X1 { p.e->q; X1 } main {X1} || def X1 { p.e->q; X1 } main {X1}";

        assertEquals(expected, actual);
    }*/
}