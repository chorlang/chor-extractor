package extraction;

import org.junit.jupiter.api.Test;
import utility.networkRefactor.NetworkUnfolder;


class UnfolderTest {
    private void unfold(String orig) {
        var unfolded1 = NetworkUnfolder.compute(orig, 0.2, 3);
        var unfolded2 = NetworkUnfolder.compute(orig, 0.4, 2);
        var unfolded3 = NetworkUnfolder.compute(orig, 0.7, 1);
        System.out.println(String.format("Original: %s\nUnfolded(0.2, 3): %s\nUnfolded(0.4, 2): %s\nUnfolded(0.7, 1): %s", orig, unfolded1, unfolded2, unfolded3));

    }

    @Test
    void ex2() {
        var orig = "c { def X {a!<pwd>; a&{ok: s?; stop, ko: X}} main {X}} | " +
                "a { def X {c?; s?; if s then c+ok; s+ok; stop else c+ko; s+ko; X} main {X}} | " +
                "s { def X {a!<s>; a&{ok: c!<t>; stop, ko:X}} main {X}}";
        unfold(orig);
    }

    @Test
    void ex4() {
        var test = "p { def X {q!<e1>; X} main {X}} | " +
                "q { def Y {p?; Y} main {Y}} | " +
                "r { def Z {s!<e2>; Z} main {Z}} | " +
                "s { def W {r?; W} main {W}}";
        unfold(test);
    }

    @Test
    void l1() {
        var test = "p { def X {q!<e1>; q!<e2>; q!<e3>; q!<e4>; q!<e5>; X} main {X}} | " +
                "q { def Y {1?; 2?; q!<e1>; 3?; q!<e2>; Y} main {p?; Y}}";
        unfold(test);
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
        unfold(test);
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

        unfold(test);
    }

    @Test
    void instrumentControlling() {
        var test =
                "user{def X{instrument+move; instrument+photo; instrument+quit; stop} " +
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

        unfold(test);
    }
}