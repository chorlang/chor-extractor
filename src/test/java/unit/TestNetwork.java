package unit;

import extraction.network.*;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import parsing.Parser;

/**
 * Unit test for classes of the network package.
 */
public class TestNetwork {
    @Test
    public void continuationStackHash(){
        //An input with many continuations on branching behaviours
        String network =
                "a { def X{b?; c!<m>;} main{if e then stop else b&{ko: stop, ok: X; b!<m>; c?;}; b?; c?; continue b!<m>; stop} }";
        //A network that should be equivalent to the above after certain reductions
        String subnetwork =
                "a{ def X{b?; c!<m>;} main{b?; c!<m>; b!<m>; c?; b?; c?; b!<m>; stop}}";
        Network n = Parser.stringToNetwork(network);
        ProcessTerm a = n.processes.get("a");
        a.reduce(false);
        a.reduce("ok");
        n.unfold();

        Network subn = Parser.stringToNetwork(subnetwork);

        //Assert the continuations that should now be on the ContinuationStack correctly count towards the hashcode.
        assertEquals(n.hashCode(), subn.hashCode());

        //Continue reducing until a continuation is popped off the stack, to check the hashcode gets updated correctly.
        a.reduce();
        subn.processes.get("a").reduce();
        a.reduce();
        subn.processes.get("a").reduce();
        a.reduce();
        subn.processes.get("a").reduce();
        assertEquals(n.hashCode(), subn.hashCode());
    }

}
