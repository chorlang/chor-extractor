package endpointprojection;

import extraction.Extraction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

//I'm assuming that assertEquals need the extracted choreography as a string

class ProjectThenExtractTests {
    @Test
    public void tst1() {
        var test = "def X {" +
                "q.e->p; if p.e " +
                "then p->q[ok]; q->r[ok]; X " +
                "else p->q[ko]; q->r[ko]; Y } " +
                "def Y {" +
                "q.e->r; if r.e " +
                "then r->q[ok]; r->p[ok]; q.e->r; stop " +
                "else r->q[ko]; r->p[ko]; Y}" +
                "main {p.e->q;X}";

        var actual = Extraction.extractChoreography(test);

        var expected =
                "def X1 { if p.e then p->q[ok]; q->r[ok]; q.e->p; X1 else p->q[ko]; q->r[ko]; X2 } " +
                        "def X2 { q.e->r; if r.e then r->q[ok]; r->p[ok]; q.e->r; stop else r->q[ko]; r->p[ko]; X2 } " +
                        "main {p.e->q; q.e->p; X1}";

        assertEquals(expected, actual.toString());
    }

    @Test
    public void tst2() {
        /* var test =
                "def X {if p.e " +
                    "then p->q[ok]; p->r[ok]; if r.e " +
                        "then q.e->p; r->p[ok];r->q[ok];p.e->q;X " +
                        "else q.e->p; r->p[ko];r->q[ko];r.u->q;Y " +
                    "else p->q[ko]; p->r[ko]; if q.e " +
                        "then q->p[ok];q->r[ok];p.e->q;X " +
                        "else q->p[ko];q->r[ko];Z } " +
                "def Y {p.e->q; X}" +
                "def Z {p.e->q; Y}" +
                "main {q.i->r; p.e->q; X}";

        var extraction = EndPointProjection.project(test).toString() */

        var test = "p{def X{if e then q + ok; r + ok; q?; r&{ko: Y, ok: q!<e>; X} else q + ko; r + ko; q&{ko: Z, ok: q!<e>; X}} def Y{q!<e>; X} def Z{q!<e>; Y} main {q!<e>; X}} | " +
                " q{def X{p&{ko: if e then p + ok; r + ok; p?; X else p + ko; r + ko; Z, ok: p!<e>; r&{ko: r?; Y, ok: p?; X}}} def Y{p?; X} def Z{p?; Y} main {r!<i>; p?; X}} |" +
                "r{def X{p&{ko: q&{ko: X, ok: X}, ok: if e then p + ok; q + ok; X else p + ko; q + ko; q!<u>; X}} main {q?; X}}";


        var actual = Extraction.extractChoreography(test);

        var expected = "def X1 { p.e->q; X2 } " +
                "def X2 { if p.e then p->q[ok]; p->r[ok]; q.e->p; if r.e then r->p[ok]; r->q[ok]; X1 else r->p[ko]; r->q[ko]; r.u->q; p.e->q; X2 else p->q[ko]; X3 } " +
                "def X3 { if q.e then p->r[ko]; q->p[ok]; q->r[ok]; p.e->q; if p.e then p->q[ok]; p->r[ok]; q.e->p; if r.e then r->p[ok]; r->q[ok]; X1 else r->p[ko]; r->q[ko]; r.u->q; p.e->q; X2 else p->q[ko]; X3 else p->r[ko]; q->p[ko]; q->r[ko]; p.e->q; p.e->q; if p.e then p->q[ok]; p->r[ok]; q.e->p; if r.e then r->p[ok]; r->q[ok]; X1 else r->p[ko]; r->q[ko]; r.u->q; p.e->q; X2 else p->q[ko]; X3 } " +
                "main {q.i->r; X1}";

        assertEquals(expected, actual.toString());
    }
}