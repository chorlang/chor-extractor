package extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ConditionTest{

    @Test
    void tst1(){ /* simple condition */
        var test =
                "p { main {if e then q!<e1>; stop else q!<e2>; stop}} " +
                        "| q { main {p?; stop}} " +
                        "| r { main {stop}}";

        var actual = Extraction.extractChoreography(test).toString();
        var expected = "main {if p.e then p.e1->q; stop else p.e2->q; stop}";

        assertEquals(expected, actual);
    }

    @Test
    void tst2(){ /* condition with selection */
        var test =
                "p { main {if e then q+R; q!<e1>; q?; stop else q+L; q!<e2>; q?; stop}} | " +
                        "q { main {p&{R: p?; p!<u1>; stop, L: p?; p!<u2>; stop}}} | " +
                        "r { main {stop}}";

        var actual = Extraction.extractChoreography(test).toString();
        var expected = "main {if p.e then p->q[R]; p.e1->q; q.u1->p; stop else p->q[L]; p.e2->q; q.u2->p; stop}";

        assertEquals(expected, actual);
    }

    @Test
    void tst3(){ /* condition inside condition */
        var test =
                "p { main {if e then if u then q!<e1>; stop else q!<e2>; stop else q!<e3>; stop}} " +
                        "| q { main {p?; stop}} " +
                        "| r { main {stop}}";

        var actual = Extraction.extractChoreography(test).toString();
        var expected = "main {if p.e then if p.u then p.e1->q; stop else p.e2->q; stop else p.e3->q; stop}";

        assertEquals(expected, actual);
    }

    @Test
    void tst4(){ /* condition with name */
        var test =
                "p {def X {if e then q!<u>;stop else q!<o>;stop} main {X}} " +
                        "| q {def Y{p?;stop} main {Y}} " +
                        "| r { main {stop}}";

        var actual = Extraction.extractChoreography(test).toString();
        var expected = "main {if p.e then p.u->q; stop else p.o->q; stop}";

        assertEquals(expected, actual);
    }

    @Test
    void tst5(){ /* condition with name */
        var test =
                "p {def X {if e then q!<u>;X else q!<o>;X} main {X}} " +
                        "| q {def Y{p?;Y} main {Y}} " +
                        "| r { main {stop}}";

        var actual = Extraction.extractChoreography(test).toString();
        var expected = "def X1 { if p.e then p.u->q; X1 else p.o->q; X1 } main {X1}";

        assertEquals(expected, actual);
    }

    @Test
    void tst6(){ /* condition with selection with recursion*/
        var test =
                "p { def X {if e then q+R; q!<e1>; q?; stop else q+L; q!<e2>; q?; stop} main {X}} | " +
                        "q { def X {p&{R: p?; p!<u1>; stop, L: p?; p!<u2>; stop}} main {X}} | " +
                        "r { main {stop}}";

        var actual = Extraction.extractChoreography(test).toString();
        var expected = "main {if p.e then p->q[R]; p.e1->q; q.u1->p; stop else p->q[L]; p.e2->q; q.u2->p; stop}";

        assertEquals(expected, actual);
    }

    @Test
    void tst7(){ /* condition with selection with recursion*/
        var test =
                "p { def X {q!<e>; X} main {X}} | " +
                        "q { def Y {p?; Y} main {Y}} | " +
                        "r { main {s!<e2>; stop}} | " +
                        "s { main {r?; stop}}";

        var actual = Extraction.extractChoreography(test).toString();
        var expected = "def X1 { p.e->q; X1 } main {X1} || main {r.e2->s; stop}";

        assertEquals(expected, actual);
    }
}