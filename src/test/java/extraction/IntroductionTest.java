package extraction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IntroductionTest {

    @Test
    void tst1(){
        var test =
                "a { main { b<->c; stop}} |" +
                "b { main { a?c; stop}} |" +
                "c { main {a?b; stop}}";
        var expected = "main {a.b<->c; stop}";
        var actual = Extraction.extractChoreography(test).toString();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void tst2(){
        var test =
                "a { main { b?; c!<msg>; c?; b<->c; b!<ask>; b?; stop}} |" +
                        "b { main { a!<req>; a?c; a?; c!<hi>; c?; a!<akn>; stop}} |" +
                        "c { main { a?; a!<akn>; a?b; b?; b!<hi>; stop}}";
        var expected = "main {b.req->a; a.msg->c; c.akn->a; a.b<->c; a.ask->b; b.hi->c; c.hi->b; b.akn->a; stop}";
        var actual = Extraction.extractChoreography(test).toString();

        Assertions.assertEquals(expected, actual);
    }
}
