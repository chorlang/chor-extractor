package extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import extraction.network.Network;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import parsing.Parser;

import java.util.List;
import java.util.Set;

/**
 * Test continuations for conditionals, offerings, and procedure invocations
 */
public class GeneralContinuationTest {
    @Test   //Tests that one conditional branch can continue after a conditional, while the other stops
    public void conditional(){
        String test =
                "a{ main{ b!<start>; if e then b+true; b?; b!<ok>; else b+false; b!<ok>; stop continue b?; b!<end>; stop } } | " +
                "b{ main { a?; a&{true: a!<ok>; a?; a!<final>; a?; stop, false: a?; stop} } }";
        String expected = "main {a.start->b; if a.e then a->b[true]; b.ok->a; a.ok->b; b.final->a; a.end->b; stop else a->b[false]; a.ok->b; stop}";
        String actual = Extraction.newExtractor().extract(test).program.toString();
        assertEquals(expected, actual);
    }

    @Test   //Test that both branches can continue after a conditional, and share a continuation.
    public void conditional2(){
        String test =
                "a{ main{ b!<start>; if e then b+true; b?; b!<ok>; else b+false; b!<ok>; continue b?; b!<end>; stop } } | " +
                "b{ main { a?; a&{true: a!<ok>; a?; a!<final>; a?; stop, false: a?; a!<final>; a?; stop} } }";
        String expected = "main {a.start->b; if a.e then a->b[true]; b.ok->a; a.ok->b; b.final->a; a.end->b; stop else a->b[false]; a.ok->b; b.final->a; a.end->b; stop}";
        String actual = Extraction.newExtractor().extract(test).program.toString();
        assertEquals(expected, actual);
    }

    @Test
    public void offering(){
        String test =
                "a{ def X{ if e then b+repeat; X else if e2 then b+send; b?; else b+receive; b!<msg>; continue b!<final>; b?; stop endif } main{ b!<start>; X } } | \n" +
                "b{ def X{ a&{repeat: X, send: a!<msg>;, receive: a?;}; a?; a!<akn>; stop } main{ a?; X } }";
        String expected = //Clearly main could just have been a.start->b; X1. Investigate
                "def X1 { if a.e then a->b[repeat]; X1 else if a.e2 then a->b[send]; b.msg->a; a.final->b; b.akn->a; stop else a->b[receive]; a.msg->b; a.final->b; b.akn->a; stop } " +
                "main {a.start->b; if a.e then a->b[repeat]; X1 else if a.e2 then a->b[send]; b.msg->a; a.final->b; b.akn->a; stop else a->b[receive]; a.msg->b; a.final->b; b.akn->a; stop}";
        String actual = Extraction.newExtractor().extract(test).program.toString();
        assertEquals(expected, actual);
    }

    @Test   //Tests that ambiguous "continue" literals of nested conditionals does not cause problems
    public void ambiguousContinuation(){
        String ambiguity =
                "a{ def X{ if e then b+send; b?; else if e2 then b+repeat; X else b+receive; b!<msg>; continue b!<final>; b?; stop } main{ b!<start>; X } } | \n" +
                "b{ def X{ a&{repeat: X, send: a!<msg>;, receive: a?;}; a?; a!<akn>; stop } main{ a?; X } }";
        Assertions.assertNull(Parser.stringToNetwork(ambiguity));   //Parsing should fail for the ambiguous input
                                                                    //This will spit out info to stderr
        String clarity =
                "a{ def X{ if e then b+send; b?; else if e2 then b+repeat; X else b+receive; b!<msg>; endif continue b!<final>; b?; stop } main{ b!<start>; X } } | \n" +
                "b{ def X{ a&{repeat: X, send: a!<msg>;, receive: a?;}; a?; a!<akn>; stop } main{ a?; X } }";
        Assertions.assertNotNull(Extraction.newExtractor().extract(clarity).program);  //Parsing should succeed with the ambiguity resolved

    }

    @Test   //Test two interacting processes both using procedure continuations
    public void invocation(){
        String test =
                "a{ def X{ if e then b+repeat; X else b+break; } main{ X; b!<1>; X; b!<2>; X; stop } } | " +
                "b{ def X{ a&{repeat: X, break:}; } main{ X; a?; X; a?; X; stop } }";
        String expected =
                "def X1 { if a.e then a->b[repeat]; X1 else a->b[break]; a.1->b; X2 } " +
                "def X2 { if a.e then a->b[repeat]; X2 else a->b[break]; a.2->b; X3 } " +
                "def X3 { if a.e then a->b[repeat]; X3 else a->b[break]; stop } " +
                "main {X1}";
        String actual = Extraction.newExtractor().extract(test).program.toString();
        assertEquals(expected, actual);
    }

    @Test   //Test two interacting processes, one of which uses procedure continuations
    public void invocation2(){
        String test =
                "a{ def X{ if e then b+repeat; X else b+break; } main{ X; b!<1>; X; b!<2>; X; b!<3>; b+end; stop } } | " +
                "b{ def X{ a&{repeat: X, break: a?; X, end: stop} } main{ X } }";
        String expected =
                "def X1 { if a.e then a->b[repeat]; X1 else a->b[break]; a.1->b; X2 } " +
                "def X2 { if a.e then a->b[repeat]; X2 else a->b[break]; a.2->b; X3 } " +
                "def X3 { if a.e then a->b[repeat]; X3 else a->b[break]; a.3->b; a->b[end]; stop } " +
                "main {X1}";
        String actual = Extraction.newExtractor().extract(test).program.toString();
        assertEquals(expected, actual);
    }

    @Test   //Test variables carry on into the continuation of conditionals
    public void varConditional(){
        String test =
                "p{ main{ if e then q+A; q?s; else q+B; q?s; continue s?; stop } } | " +
                "q{ main{ p&{A: a+ok; b+end; p<->a;, B: a+end; b+ok; p<->b;}; stop } } | " +
                "a{ main{ q&{ok: q?c; c!<infoA>; stop, end: stop} } } | " +
                "b{ main{ q&{ok: q?c; c!<infoB>; stop, end: stop} } }";
        String expected = "main {if p.e then p->q[A]; q->a[ok]; q->b[end]; q.p<->a; a.infoA->p; stop else p->q[B]; q->a[end]; q->b[ok]; q.p<->b; b.infoB->p; stop}";
        Extraction.ExtractionResult result = Extraction.newExtractor().extract(test);
        String actual = result.program.toString();
        assertEquals(expected, actual);
    }

    @Test   //Test variables carry on into the continuation of offerings
    public void varOffering(){
        String test =
                "p{ main{ if e then q+new; a+ok; q<->a; stop else q+old; a+end; q!<final>; stop } } | " +
                "q{ main{ p&{new: p?p;, old:}; p?; stop } } | " +
                "a{ main{ p&{ok: p?c; c!<final>; stop, end: stop} } }";
        String expected = "main {if p.e then p->q[new]; p->a[ok]; p.q<->a; a.final->q; stop else p->q[old]; p->a[end]; p.final->q; stop}";
        String actual = Extraction.newExtractor().extract(test).program.toString();
        assertEquals(expected, actual);
    }

    @Test
    public void varInvocation(){
        String test =
                "a{ def X{ p?c; c!<akn>; } def Y{ c?d; d!<akn>; } main{ X; c!<msg>; Y; d!<msg>; c?; stop } } | " +
                "p{ main{ spawn q with " +
                "   p?u; u?; u?; spawn r with " +
                "       q?w; w?; w?; stop " +
                "   continue r<->u; u!<fin>; stop " +
                "continue q<->a; stop } }";
        String expected =
                "main {p spawns p/q0; p.p/q0<->a; a.akn->p/q0; a.msg->p/q0; p/q0 spawns p/q0/r1; p/q0.p/q0/r1<->a; a.akn->p/q0/r1; a.msg->p/q0/r1; p/q0.fin->a; stop}";
        String actual = Extraction.newExtractor().extract(test).program.toString();
        assertEquals(expected, actual);
    }

    @Test
    public void invocationParameters(){
        String test = "a{ def X(p){ p?; p!<resp>; } def Y(q){ q?; } main{ X(b); p?; Y(p); q!<fin>; stop } } | " +
                "b{ main{ a!<msgx>; a?; a!<msg2>; a!<msgy>; a?; stop } }";
        String expected = "main {b.msgx->a; a.resp->b; b.msg2->a; b.msgy->a; a.fin->b; stop}";
        String actual = Extraction.newExtractor().extract(test).program.toString();
        assertEquals(expected, actual);
    }


}
