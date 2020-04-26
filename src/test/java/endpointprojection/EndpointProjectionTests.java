package endpointprojection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EndpointProjectionTests {

    @Test
    public void tst1(){
        var test = "def X {Y} def Y { p.e->q; stop } main {q.e->p;X}";

        var actual = EndPointProjection.project(test).toString();
        //  println(EndPointProjection.getChoreographyStatistic(f));
        var expected = "p{def X{Y} def Y{q!<e>; stop} main {q?; X}} | q{def X{Y} def Y{p?; stop} main {p!<e>; X}}";

        assertEquals(expected, actual);
    }

    @Test
    public void tst(){
        var test =
                "def T { p -> u[l3]; d -> o[l4]; K } " +
                        "def K { o.m3 -> u; o.m4 -> v; o -> p[l5]; p.m5 -> u; K } " +
                        "main { u.m1 -> v; o -> v[l1]; v -> u[l2]; u.m2 -> d; T }";
        EndPointProjection.project(test).toString();
    }



    @Test
    public void tst2(){ //termination
        var test = "main { stop }";

        var actual = EndPointProjection.project(test).toString();
        var expected = "";

        assertEquals(expected, actual);
    }

    @Test
    public void tst3(){ //finite interaction with name invocation
        var test = "def X { p.e->q;stop } main { X }";

        var actual = EndPointProjection.project(test).toString();
        var expected = "p{def X{q!<e>; stop} main {X}} | q{def X{p?; stop} main {X}}";

        assertEquals(expected, actual);
    }

    @Test
    public void tst4(){ //finite interaction
        var test = "main {p.e->q;stop}";

        var actual = EndPointProjection.project(test).toString();
        var expected = "p{main {q!<e>; stop}} | q{main {p?; stop}}";

        assertEquals(expected, actual);
    }

    @Test
    public void tst5(){ //finite interaction
        var test = "main {p->q[l];stop}";

        var actual = EndPointProjection.project(test).toString();
        var expected = "p{main {q + l; stop}} | q{main {p&{l: stop}}}";

        assertEquals(expected, actual);
    }

    @Test
    public void tst6(){ //name definition/invocation
        var test = "main {if p.e then p.e->q;stop else p.e->q; stop}";

        var actual = EndPointProjection.project(test).toString();
        var expected = "p{main {if e then q!<e>; stop else q!<e>; stop}} | q{main {p?; stop}}";

        assertEquals(expected, actual);
    }

    @Test
    public void tst7(){ //condition
        var test = "main {if p.e then if q.e2 then p.e1 -> q; stop else p.e1 -> q; stop else if q.e2 then p.e3 -> q; stop else p.e3 -> q; stop}";

        var actual = EndPointProjection.project(test).toString();
        var expected = "p{main {if e then q!<e1>; stop else q!<e3>; stop}} | q{main {if e2 then p?; stop else p?; stop}}";

        assertEquals(expected, actual);
    }

    @Test
    public void tst8(){
        var test = "main {if p.e then p -> q[L]; p.e -> q; q.x -> r; r.z -> q; stop " +
                "else p -> q[R]; q.y -> r; r.z -> q; q.u -> p; stop}";

        var actual = EndPointProjection.project(test).toString();
        var expected = "p{main {if e then q + L; q!<e>; stop else q + R; q?; stop}} | " +
                "q{main {p&{R: r!<y>; r?; p!<u>; stop, L: p?; r!<x>; r?; stop}}} | " +
                "r{main {q?; q!<z>; stop}}";

        assertEquals(expected, actual);
    }

    @Test
    public void tst9(){
        var test = "main {if p.e then p -> q[L]; p.e -> q; q -> r[L1]; r.z1 -> q; stop " +
                "else p -> q[R]; q -> r[R1]; r.z2 -> q; q.u -> p; stop}";

        var actual = EndPointProjection.project(test).toString();
        var expected = "p{main {if e then q + L; q!<e>; stop else q + R; q?; stop}} | " +
                "q{main {p&{R: r + R1; r?; p!<u>; stop, L: p?; r + L1; r?; stop}}} | " +
                "r{main {q&{L1: q!<z1>; stop, R1: q!<z2>; stop}}}";

        assertEquals(expected, actual);
    }

    @Test
    public void tst10(){
        var test = "def X {q.e->p; if p.e then p->q[ok]; q->r[ok]; X else p->q[ko]; q->r[ko]; Y } " +
                "def Y {q.e->r; if r.e then r->q[ok]; r->p[ok]; q.e->r; stop else r->q[ko]; r->p[ko]; Y}" +
                "main {p.e->q;X}";

        var actual = EndPointProjection.project(test).toString();
        var expected = "p{" +
                "def X{q?; if e then q + ok; X else q + ko; Y} " +
                "def Y{r&{ko: Y, ok: stop}} " +
                "main {q!<e>; X}} | " +
                "q{" +
                "def X{p!<e>; p&{ko: r + ko; Y, ok: r + ok; X}} " +
                "def Y{r!<e>; r&{ko: Y, ok: r!<e>; stop}} " +
                "main {p?; X}} | " +
                "r{" +
                "def X{q&{ko: Y, ok: X}} " +
                "def Y{q?; if e then q + ok; p + ok; q?; stop else q + ko; p + ko; Y} " +
                "main {X}}";

        assertEquals(expected, actual);
    }

    @Test
    public void tst11(){
        var test = "def X {if p.e " +
                "then p->q[ok]; p->r[ok]; if r.e " +
                "then q.e->p; r->p[ok];r->q[ok];p.e->q;X " +
                "else q.e->p; r->p[ko];r->q[ko];r.u->q;Y " +
                "else p->q[ko]; p->r[ko]; if q.e " +
                "then q->p[ok];q->r[ok];p.e->q;X " +
                "else q->p[ko];q->r[ko];Z } " +
                "def Y {p.e->q; X}" +
                "def Z {p.e->q; Y}" +
                "main {q.i->r; p.e->q; X}";

        var actual = EndPointProjection.project(test).toString();
        var expected = "p{" +
                "def X{if e then q + ok; r + ok; q?; r&{ko: Y, ok: q!<e>; X} else q + ko; r + ko; q&{ko: Z, ok: q!<e>; X}} " +
                "def Y{q!<e>; X} " +
                "def Z{q!<e>; Y} " +
                "main {q!<e>; X}} | " +
                "q{" +
                "def X{p&{ko: if e then p + ok; r + ok; p?; X else p + ko; r + ko; Z, ok: p!<e>; r&{ko: r?; Y, ok: p?; X}}} " +
                "def Y{p?; X} " +
                "def Z{p?; Y} " +
                "main {r!<i>; p?; X}} | " +
                "r{" +
                "def X{p&{ko: q&{ko: Z, ok: X}, ok: if e then p + ok; q + ok; X else p + ko; q + ko; q!<u>; Y}} " +
                "def Y{X} " +
                "def Z{Y} main {q?; X}}";

        assertEquals(expected, actual);
    }

    @Test //(expected = Merging.MergingException::class);
    public void tst12() {
        var test =
                "def X1 { " +
                        "p.e->q; if p.e " +
                        "then X2 " +
                        "else X4 } " +
                        "def X2 { " +
                        "p->q[ok]; p->r[ok]; q.e->p; if r.e " +
                        "then r->p[ok]; r->q[ok]; X1 " +
                        "else r->p[ko]; r->q[ko]; r.u->q; p.e->q; if p.e " +
                        "then X2 " +
                        "else X3 } " +
                        "def X3 { " +
                        "p->q[ko]; p->r[ko]; if q.e " +
                        "then q->p[ok]; q->r[ok]; X1 " +
                        "else q->p[ko]; q->r[ko]; p.e->q; p.e->q; if p.e " +
                        "then X2 " +
                        "else X3 } " +
                        "def X4 { " +
                        "p->q[ko]; p->r[ko]; if q.e " +
                        "then q->p[ok]; q->r[ok]; X1 " +
                        "else q->p[ko]; q->r[ko]; p.e->q; p.e->q; if p.e " +
                        "then X5 " +
                        "else X4 } " +
                        "def X5 { " +
                        "p->q[ok]; p->r[ok]; q.e->p; if r.e " +
                        "then r->p[ok]; r->q[ok]; X1 " +
                        "else r->p[ko]; r->q[ko]; r.u->q; p.e->q; if p.e " +
                        "then X5 " +
                        "else X4 } " +
                        "main {q.i->r; X1}";



        Assertions.assertThrows(Merging.MergingException.class, () -> EndPointProjection.project(test));

    }
    @Test
    public void tst13(){
        var test = "def X {Y} def Y { p.e->q; stop } main {q.e->p;X} || def X {Y} def Y { p.e->q; stop } main {q.e->p;X}";

        var actual = EndPointProjection.project(test).toString();
        var expected = "p{def X{Y} def Y{q!<e>; stop} main {q?; X}} | q{def X{Y} def Y{p?; stop} main {p!<e>; X}} || p{def X{Y} def Y{q!<e>; stop} main {q?; X}} | q{def X{Y} def Y{p?; stop} main {p!<e>; X}}";

        //This also causes InputMismatchException in the Kotlin implementations
        assertEquals(expected, actual);
    }
}
