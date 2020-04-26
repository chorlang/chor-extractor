package fuzz;

import org.junit.jupiter.api.Test;
import utility.fuzzing.NetworkFuzzer;


//Some of these test may throw IllegalStateException
public class FuzzTest {
    private void testFuzz(String orig, int dels, int swaps){
        var fuzzed1 = NetworkFuzzer.fuzz(orig, 1, 0);
        var fuzzed2 = NetworkFuzzer.fuzz(orig, 0, 1);
        var fuzzed3 = NetworkFuzzer.fuzz(orig, dels, swaps);
        System.out.println(String.format("Original: %s\nFuzzed(1, 0): %s\nFuzzed(0, 1): %s\nFuzzed(%d, %d): %s", orig, fuzzed1, fuzzed2, dels, swaps, fuzzed3));


    }

    @Test
    void ex2() {
        var orig = "c { def X {a!<pwd>; a&{ok: s?; stop, ko: X}} main {X}} | " +
                "a { def X {c?; s?; if s then c+ok; s+ok; stop else c+ko; s+ko; X} main {X}} | " +
                "s { def X {a!<s>; a&{ok: c!<t>; stop, ko:X}} main {X}}";
        testFuzz(orig, 2, 1);
    }

    @Test
    void ex4() {
        var test = "p { def X {q!<e1>; X} main {X}} | " +
                "q { def Y {p?; Y} main {Y}} | " +
                "r { def Z {s!<e2>; Z} main {Z}} | " +
                "s { def W {r?; W} main {W}}";
        testFuzz(test, 1, 1);
    }

    @Test
    void l1() {
        var test = "p { def X {q!<e1>; q!<e2>; q!<e3>; q!<e4>; q!<e5>; X} main {X}} | " +
                "q { def Y {1?; 2?; q!<e1>; 3?; q!<e2>; Y} main {p?; Y}}";
        testFuzz(test, 2, 2);
    }

    @Test
    void buyerSeller() {
        var test =
                "buyer{main{seller!<quote>; seller?; if ok then seller+accept; seller?; stop else seller+reject; stop}} | " +
                        "shipper{main{seller&{" +
                        "send: seller?; seller!<t>; stop," +
                        "wait: stop}}} | " +
                        "seller{main{buyer?; buyer!<quote>; buyer&{" +
                        "accept: shipper+send; shipper!<deliv>; shipper?; buyer!<details>; stop, " +
                        "reject: shipper+wait; stop}}}";
        testFuzz(test, 2, 2);
    }

    @Test
    void buyerSellerRec(){
        var test =
                "buyer{def X {seller?; if ok then seller+accept; seller?; stop else seller+reject; X} main {seller!<quote>; X}} | " +
                        "shipper{def X {seller&{" +
                        "send: seller?; seller!<t>; stop," +
                        "wait: X}} main {X}} | " +
                        "seller{def X {buyer!<quote>; buyer&{" +
                        "accept: shipper+send; shipper!<deliv>; shipper?; buyer!<details>; stop, " +
                        "reject: shipper+wait; X}} main {buyer?; X}}";

        testFuzz(test, 2, 3);
    }

    @Test
    void instrumentControlling() {
        var test = "user{def X{instrument+move; instrument+photo; instrument+quit; stop} " +
                "main {operator!<high>; operator&{" +
                "ok: X," +
                "no: stop}}} | " +
                "operator{main{user?; if ok then user+ok; instrument+ok; stop else user+no; instrument+no; stop}} | " +
                "instrument{def X{user&{" +
                "move: X," +
                "photo: X," +
                "quit: stop}} main{ operator&{" +
                "ok: X, " +
                "no: stop}}}";

        testFuzz(test, 2, 2);
    }
}
