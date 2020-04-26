package bisim;

import org.junit.jupiter.api.Test;
import static utility.Bisimulation.bisimilar;
import utility.Bisimulation.Throolean;




class BisimTest{

    @Test
    void same1(){
        var test = "def X {Y} def Y { p.e->q; stop } main {q.e->p;X}";
        assert( Throolean.OK == bisimilar(test,test) );
    }

    @Test
    void same2(){
        var test =
                "def T { p -> u[l3]; d -> o[l4]; K } " +
                        "def K { o.m3 -> u; o.m4 -> v; o -> p[l5]; p.m5 -> u; K } " +
                        "main { u.m1 -> v; o -> v[l1]; v -> u[l2]; u.m2 -> d; T }";
        assert( Throolean.OK ==  bisimilar(test,test) );
    }

    @Test
    void same3(){ //termination
        var test = "main { stop }";

        assert( Throolean.OK ==  bisimilar(test,test) );
    }

    @Test
    void same4(){
        var test = "def X { p.e->q;stop } main { X }";
        assert( Throolean.OK ==  bisimilar(test,test) );
    }

    @Test
    void same5(){ //finite interaction
        var test = "main {p.e->q;stop}";
        assert( Throolean.OK ==  bisimilar(test,test) );
    }

    @Test
    void same6(){ //finite interaction
        var test = "main {p->q[l];stop}";

        assert( Throolean.OK ==  bisimilar(test,test) );
    }

    @Test
    void same7(){
        var test = "main {if p.e then p.e->q;stop else p.e->q; stop}";

        assert( Throolean.OK ==  bisimilar(test,test) );
    }

    @Test
    void same8(){
        var test = "main {if p.e then if q.e2 then p.e1 -> q; stop else p.e1 -> q; stop else if q.e2 then p.e3 -> q; stop else p.e3 -> q; stop}";

        assert( Throolean.OK ==  bisimilar(test,test) );
    }

    @Test
    void same9(){
        var test = "main {if p.e then p -> q[L]; p.e -> q; q.x -> r; r.z -> q; stop " +
                "else p -> q[R]; q.y -> r; r.z -> q; q.u -> p; stop}";
        assert( Throolean.OK ==  bisimilar(test,test) );
    }

    @Test
    void same10(){
        var test = "main {if p.e then p -> q[L]; p.e -> q; q -> r[L1]; r.z1 -> q; stop " +
                "else p -> q[R]; q -> r[R1]; r.z2 -> q; q.u -> p; stop}";

        assert( Throolean.OK ==  bisimilar(test,test) );
    }

    @Test
    void same11(){
        var test = "def X {q.e->p; if p.e then p->q[ok]; q->r[ok]; X else p->q[ko]; q->r[ko]; Y } " +
                "def Y {q.e->r; if r.e then r->q[ok]; r->p[ok]; q.e->r; stop else r->q[ko]; r->p[ko]; Y}" +
                "main {p.e->q;X}";

        assert( Throolean.OK ==  bisimilar(test,test) );
    }

    @Test
    void same12(){
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

        assert( Throolean.OK ==  bisimilar(test,test) );
    }

    @Test // This is unmergeable
    void same13() {
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



        assert( Throolean.OK ==  bisimilar(test,test) );
    }

    @Test
    void same14(){
        var test = "def X {Y} def Y { p.e->q; stop } main {q.e->p;X} || def X {Y} def Y { r.e->s; stop } main {s.e->r;X}";

        assert( Throolean.MAYBE ==  bisimilar(test,test) );
    }

    @Test
    void diffExpr(){
        var c1 = "main { p.e1 -> q; stop }";
        var c2 = "main { p.e2 -> q; stop }";

        assert( Throolean.FAIL ==  bisimilar(c1,c2) );
    }

    @Test
    void diffProcs(){
        var c1 = "main { p.e -> q; stop }";
        var c2 = "main { p.e -> q2; stop }";

        assert( Throolean.FAIL ==  bisimilar(c1,c2) );
    }

    @Test
    void diffLabel(){
        var c1 = "main { p -> q[l1]; stop }";
        var c2 = "main { p -> q[l2]; stop }";

        assert( Throolean.FAIL ==  bisimilar(c1,c2) );
    }

    @Test
    void swap1(){
        var c1 = "main { p1.e1 -> q1; p2.e2 -> q2; stop }";
        var c2 = "main { p2.e1 -> q2; p1.e2 -> q1; stop }";

        // e should be e2 in c2 and vice versa

        assert( Throolean.FAIL ==  bisimilar(c1,c2) );
    }

    @Test
    void swap2(){
        var c1 = "main { p1.e1 -> q1; p2.e2 -> q2; stop }";
        var c2 = "main { p2.e2 -> q2; p1.e1 -> q1; stop }";

        assert( Throolean.OK ==  bisimilar(c1,c2) );
    }

    @Test
    void swap3(){
        var c1 = "main { p1.e1 -> q1; p1.e2 -> q2; stop }";
        var c2 = "main { p1.e2 -> q2; p1.e1 -> q1; stop }";

        assert( Throolean.FAIL ==  bisimilar(c1,c2) );
    }

    @Test
    void swap4(){
        var c1 = "main { p1.e1 -> q1; q1.e3 -> r; p2.e2 -> q2; stop }";
        var c2 = "main { p2.e2 -> q2; q1.e3 -> r; p1.e1 -> q1; stop }";

        assert( Throolean.FAIL ==  bisimilar(c1,c2) );
    }

    @Test
    void swap5(){
        var c1 = "main { p1.e1 -> q1; p2.e2 -> q2; p2.e2 -> q2; stop }";
        var c2 = "main { p2.e2 -> q2; p2.e2 -> q2; p1.e1 -> q1; stop }";

        assert( Throolean.OK ==  bisimilar(c1,c2) );
    }

    @Test
    void swapCondCom(){
        var c1 = "main { if p.e then r.e -> s; stop else r.e -> s; stop }";
        var c2 = "main { r.e -> s; if p.e then stop else stop }";

        assert( Throolean.OK ==  bisimilar(c1,c2) );
    }

    @Test
    void swapCondWrong(){
        var c1 = "main { if s.e then r.e -> s; stop else r.e -> s; stop }";
        var c2 = "main { r.e -> s; if s.e then stop else stop }";

        assert( Throolean.FAIL ==  bisimilar(c1,c2) );
    }

    @Test
    void swapCondCond(){
        var c1 = "main { if p.e then if q.e then stop else stop else if q.e then stop else stop }";
        var c2 = "main { if q.e then if p.e then stop else stop else if p.e then stop else stop }";

        assert( Throolean.OK ==  bisimilar(c1,c2) );
    }

    @Test
    void swapCondCondWrong(){
        var c1 = "main { if p.e1 then if q.e2 then stop else stop else if q.e2 then stop else stop }";
        var c2 = "main { if q.e1 then if p.e1 then stop else stop else if p.e1 then stop else stop }";

        assert( Throolean.FAIL ==  bisimilar(c1,c2) );
    }

    @Test
    void swapCondCondWithProcedure(){
        var c1 = "def X { if q.e then stop else stop } main { if p.e then X else X }";
        var c2 = "main { if q.e then if p.e then stop else stop else if p.e then stop else stop }";

        assert( Throolean.OK ==  bisimilar(c1,c2) );
    }

    @Test
    void swapCondCondWithProcedureInfinite(){
        var c1 = "def X { if q.e then X else X } main { if p.e then X else X }";
        var c2 = "def X { if q.e then X else X } main { if q.e then if p.e then X else X else if p.e then X else X }";

        assert( Throolean.OK ==  bisimilar(c1,c2) );
    }

    @Test
    void swapCondCondMutuallyRecursive(){
        var c1 = "def Y { if p.e then X else X } def X { if q.e then Y else Y } main { if p.e then X else X }";
        var c2 = "def X { if p.e then Y else Y } def Y { if q.e then X else X } main { if q.e then if p.e then Y else Y else X }";

        assert( Throolean.OK ==  bisimilar(c1,c2) );
    }

    @Test
    void swapCondCondComMutuallyRecursive(){
        var c1 = "def Y { if p.e then X else X } def X { r.e -> s; if q.e then Y else Y } main { if p.e then X else X }";
        var c2 = "def X { r.e -> s; if p.e then Y else Y } def Y { if q.e then X else X } main { if q.e then if p.e then r.e -> s; Y else r.e -> s; Y else X }";

        assert( Throolean.OK ==  bisimilar(c1,c2) );
    }

    @Test
    void unfolding(){
        var c1 = "def X { p.e -> q; p.e -> q; X } main { X }";
        var c2 = "def X { p.e -> q; p.e -> q; p.e -> q; X } main { X }";

        assert( Throolean.OK ==  bisimilar(c1,c2) );
    }

    @Test
    void C171(){
        var C171orig = "def H { s.m4 -> n; j.m5 -> x; x -> j[l3]; v.m6 -> s; s -> v[l4]; H } main { j.m1 -> v; j.m2 -> s; v -> n[l1]; v -> s[l2]; v.m3 -> s; H }";
        var C171extracted = "def X1 { s.m4->n; j.m5->x; x->j[l3]; v.m6->s; s->v[l4]; X1 } main {j.m1->v; v->n[l1]; j.m2->s; v->s[l2]; v.m3->s; j.m5->x; x->j[l3]; j.m5->x; s.m4->n; x->j[l3]; v.m6->s; s->v[l4]; j.m5->x; x->j[l3]; X1}";
        assert( Throolean.OK ==  bisimilar(C171orig, C171extracted) );
    }

    @Test
    void simplified_C1998_problem(){
        var orig = "def X { p.e -> q; r.e -> s; X } main { X }";
        var extracted = "def X { p.e -> q; p.e -> q; r.e -> s; X } main { X }";

        assert( Throolean.MAYBE ==  bisimilar(orig, extracted) );
    }

    @Test
    void C1998(){
        var orig = "def H { v.m2 -> s; v -> n[l2]; if x.c4 then n -> v[l3]; s -> j[l4]; H else n -> v[l3]; s -> j[l4]; H }\n" +
                "main { if v.c1 then if j.c2 then j -> n[L]; j -> x[L]; j -> v[L]; j -> s[L]; v -> n[L]; v -> j[L]; v -> x[L]; v -> s[L]; n.m1 -> x; if n.c3 then H else H else j -> n[R]; j -> x[R]; j -> v[R]; j -> s[R]; v -> n[L]; v -> j[L]; v -> x[L]; v -> s[L]; n.m1 -> x; if n.c3 then H else H else if j.c2 then j -> n[L]; j -> x[L]; j -> v[L]; j -> s[L]; v -> n[R]; v -> j[R]; v -> x[R]; v -> s[R]; n -> v[l1]; 0 else j -> n[R]; j -> x[R]; j -> v[R]; j -> s[R]; v -> n[R]; v -> j[R]; v -> x[R]; v -> s[R]; n -> v[l1]; 0 }";
        var extracted = "def X1 { s->j[l4]; if x.c4 then if x.c4 then v->n[l2]; n->v[l3]; if x.c4 then v.m2->s; X1 else v.m2->s; X1 else v->n[l2]; n->v[l3]; if x.c4 then v.m2->s; X1 else v.m2->s; X1 else if x.c4 then v->n[l2]; n->v[l3]; if x.c4 then v.m2->s; X1 else v.m2->s; X1 else v->n[l2]; n->v[l3]; if x.c4 then v.m2->s; X1 else v.m2->s; X1 } def X2 { s->j[l4]; if x.c4 then if x.c4 then v->n[l2]; n->v[l3]; if x.c4 then v.m2->s; X2 else v.m2->s; X2 else v->n[l2]; n->v[l3]; if x.c4 then v.m2->s; X2 else v.m2->s; X2 else if x.c4 then v->n[l2]; n->v[l3]; if x.c4 then v.m2->s; X2 else v.m2->s; X2 else v->n[l2]; n->v[l3]; if x.c4 then v.m2->s; X2 else v.m2->s; X2 } def X3 { s->j[l4]; if x.c4 then if x.c4 then v->n[l2]; n->v[l3]; if x.c4 then v.m2->s; X3 else v.m2->s; X3 else v->n[l2]; n->v[l3]; if x.c4 then v.m2->s; X3 else v.m2->s; X3 else if x.c4 then v->n[l2]; n->v[l3]; if x.c4 then v.m2->s; X3 else v.m2->s; X3 else v->n[l2]; n->v[l3]; if x.c4 then v.m2->s; X3 else v.m2->s; X3 } def X4 { s->j[l4]; if x.c4 then if x.c4 then v->n[l2]; n->v[l3]; if x.c4 then v.m2->s; X4 else v.m2->s; X4 else v->n[l2]; n->v[l3]; if x.c4 then v.m2->s; X4 else v.m2->s; X4 else if x.c4 then v->n[l2]; n->v[l3]; if x.c4 then v.m2->s; X4 else v.m2->s; X4 else v->n[l2]; n->v[l3]; if x.c4 then v.m2->s; X4 else v.m2->s; X4 } main {if v.c1 then if j.c2 then j->n[L]; j->x[L]; j->v[L]; j->s[L]; v->n[L]; v->j[L]; v->x[L]; v->s[L]; n.m1->x; if n.c3 then v.m2->s; X1 else v.m2->s; X2 else j->n[R]; j->x[R]; j->v[R]; j->s[R]; v->n[L]; v->j[L]; v->x[L]; v->s[L]; n.m1->x; if n.c3 then v.m2->s; X3 else v.m2->s; X4 else if j.c2 then j->n[L]; j->x[L]; j->v[L]; j->s[L]; v->n[R]; v->j[R]; v->x[R]; v->s[R]; n->v[l1]; stop else j->n[R]; j->x[R]; j->v[R]; j->s[R]; v->n[R]; v->j[R]; v->x[R]; v->s[R]; n->v[l1]; stop}";

        assert( Throolean.MAYBE ==  bisimilar(orig, extracted) );
    }

//    Stack overflows
//    @Test
//    void unfolding2();
//    {
//        var c1 = "def X { p.e -> q; p.e -> q; Y } def Y { r.e -> s; X } main { X }";
//        var c2 = "def X { p.e -> q; p.e -> q; p.e -> q; Y } def Y { r.e -> s; X } main { X }";
//
////        assert( Throolean.FAIL ==  similar(c2,c1) );
////        assert( Throolean.FAIL ==  similar(c1,c2) );
//
////        assert( Throolean.FAIL ==  bisimilar(c1,c2) );
//    }

    @Test // This is unmergeable
    void mix() {
        var c1 =
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

        var c2 =
                "def X1 { " +
                        "p.e->q; if p.e " +
                        "then X2 " +
                        "else X4 } " +
                        "def X2 { " +
                        "p->q[ok]; p->r[ok]; if r.e " +
                        "then  q.e->p; r->p[ok]; r->q[ok]; X1 " +
                        "else q.e->p; r->p[ko]; r->q[ko]; r.u->q; p.e->q; if p.e " +
                        "then X2 " +
                        "else X3 } " +
                        "def X3 { " +
                        "p->q[ko];  if q.e " +
                        "then p->r[ko]; q->p[ok]; q->r[ok]; X1 " +
                        "else p->r[ko]; q->p[ko]; q->r[ko]; p.e->q; p.e->q; if p.e " +
                        "then X2 " +
                        "else X3 } " +
                        "def X4 { " +
                        "p->q[ko]; if q.e " +
                        "then p->r[ko]; q->p[ok]; q->r[ok]; X1 " +
                        "else p->r[ko]; q->p[ko]; q->r[ko]; p.e->q; p.e->q; if p.e " +
                        "then X5 " +
                        "else X4 } " +
                        "def X5 { " +
                        "p->q[ok]; p->r[ok]; if r.e " +
                        "then q.e->p; r->p[ok]; r->q[ok]; X1 " +
                        "else q.e->p; r->p[ko]; r->q[ko]; r.u->q; p.e->q; if p.e " +
                        "then X5 " +
                        "else X4 } " +
                        "main {q.i->r; X1}";

        assert( Throolean.OK ==  bisimilar(c1,c2) );
    }
}
