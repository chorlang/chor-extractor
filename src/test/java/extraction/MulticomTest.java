package extraction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MulticomTest {

    @Test
    void tst1(){
        var test =
                "a { main {b!<msg1>; b?; stop} } | " +
                "b { main {a!<msg2>; a?; stop} }";
        var expected = "main {(a.msg1->b, b.msg2->a); stop}";
        var actual = Extraction.extractChoreography(test).toString();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void tst2(){
        var test =
                "a { main {b?; b!<msg1>; b?; c?; stop} } |" +
                "b { main {a!<pre>; a!<msg2>; c!<msg3>; a?; stop} } |" +
                "c { main {b?; a!<post>; stop} }";
        var expected = "main {b.pre->a; (a.msg1->b, b.msg2->a, b.msg3->c); c.post->a; stop}";
        var actual = Extraction.extractChoreography(test).toString();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void alt2bit(){
        var test =
                "a { def X {b?; b!<0>; b?; b!<1>; X} main {b!<0>; b!<1>; X} } |" +
                "b { def Y {a?; a!<ack0>; a?; a!<ack1>; Y} main {Y} }";
        var expected = "def X1 { (a.1->b, b.ack0->a); (a.0->b, b.ack1->a); X1 } main {a.0->b; X1}";
        var actual = Extraction.extractChoreography(test).toString();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void introduction(){
        var test =
                "a { main { b<->c; b?; stop}} |" +
                "b { main { a!<msg>; a?c; c!<hello>; stop}} |" +
                "c {main { a?b; b?; stop}}";
        var expected = "main {(b.msg->a, a.c<->b); b.hello->c; stop}";
        var actual = Extraction.extractChoreography(test).toString();

        Assertions.assertEquals(expected, actual);
    }
}
