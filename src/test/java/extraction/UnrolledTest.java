package extraction;

import org.junit.jupiter.api.Test;

class UnrolledTest {

    @Test
    void originalUnrolledC42(){
        var test = 
"s{" +
    "def P{H}" +
    "def Q{x&{R: v&{R: H, L: P}, L: Q}}" +
    "def D{if c6 then n + L; j + L; x + L; v + L; H else n + R; j + R; x + R; v + R; v!<m8>; Q}" +
    "def H{v&{R: H, L: v&{R: D, L: H}}}" +
    "main {j!<m1>; j!<m2>; x&{R: D, L: v&{R: H, L: Q}}}" +
"}" +
"|" +
"v{" +
    "def P{x&{l1: H}}" +
    "def Q{x&{R: if c5 then n + L; j + L; x + L; s + L; P else n + R; j + R; x + R; s + R; H, L: Q}}" +
    "def D{x&{l2: s&{R: s?; Q, L: H}}}" +
    "def H{if c7 then n + L; j + L; x + L; s + L; if c8 then n + L; j + L; x + L; s + L; H else n + R; j + R; x + R; s + R; D else n + R; j + R; x + R; s + R; H}" +
    "main {x&{R: D, L: if c2 then n + L; j + L; x + L; s + L; Q else n + R; j + R; x + R; s + R; H}}" +
"}" +
"|" +
"x{" +
    "def P{v + l1; H}" +
    "def Q{if c3 then j!<m4>; n + L; j + L; v + L; s + L; Q else j!<m4>; n + R; j + R; v + R; s + R; j!<m7>; v&{R: H, L: j!<m3>; P}}" +
    "def D{n&{l3: s&{R: n&{l4: Q}, L: H}}} def H{v&{R: H, L: v&{R: v + l2; D, L: H}}}" +
    "main {if c1 then n + L; j + L; v + L; s + L; v&{R: H, L: Q} else n + R; j + R; v + R; s + R; v + l2; D}" +
"}" +
"|" +
"j{" +
    "def P{H}" +
    "def Q{x&{R: n!<m5>; n?; x?; v&{R: H, L: x?; P}, L: x?; Q}}" +
    "def D{s&{R: x?; Q, L: H}}" +
    "def H{v&{R: H, L: v&{R: D, L: H}}}" +
    "main {s?; s?; x&{R: D, L: v&{R: H, L: x?; Q}}}" +
"}" +
"|" +
"n{" +
    "def P{H}" +
    "def Q{x&{R: j?; j!<m6>; v&{R: H, L: P}, L: if c4 then Q else Q}}" +
    "def D{x + l3; s&{R: x + l4; Q, L: H}}" +
    "def H{v&{R: H, L: v&{R: D, L: H}}}" +
    "main {x&{R: D, L: v&{R: H, L: Q}}}" +
"}";
        var actual = Extraction.extractChoreography(test).toString();
        System.out.println(actual);
    }

    @Test
    void simplifiedUnrolledC42(){
        var test =
"v{" +
    "def P{x&{l1: H}}" +
    "def Q{x&{R: if c5 then n + L; x + L; P else n + R; x + R; H, L: Q}}" +
    "def D{x&{l2: H}}" +
    "def H{if c7 then n + L; x + L; if c8 then n + L; x + L; H else n + R; x + R; D else n + R; x + R; H}" +
    "main {x&{R: D, L: if c2 then n + L; x + L; Q else n + R; x + R; H}}" +
"}" +
"|" +
"x{" +
    "def P{v + l1; H}" +
    "def Q{if c3 then n + L; v + L; Q else n + R; v + R; v&{R: H, L: P}}" +
    "def D{n&{l3: H}} def H{v&{R: H, L: v&{R: v + l2; D, L: H}}}" +
    "main {if c1 then n + L; v + L; v&{R: H, L: Q} else n + R; v + R;v + l2; D}" +
"}" +
"|" +
"n{" +
    "def P{H}" +
    "def Q{x&{R: v&{R: H, L: P}, L: if c4 then Q else Q}}" +
    "def D{x + l3; H}" +
    "def H{v&{R: H, L: v&{R: D, L: H}}}" +
    "main {x&{R: D, L: v&{R: H, L: Q}}}" +
"}";
        var actual = Extraction.extractChoreography(test).toString();
        System.out.println(actual);
    }

    @Test
    void C112modded() {
        var test =
"v{def V{x?; V} main{V}} |" +
"x{def V{j&{l4: v!<m4>; V}} main{V}} |" +
"j{def V{x + l4; V} main{V} }";
        var actual = Extraction.extractChoreography(test).toString();
        System.out.println(actual);
    }

    @Test
    void originalC112(){
        var test =
"s{" +
    "def P{n&{R: H, L: X}}" +
    "def Q{if c4 then n + L; j + L; x + L; v + L; X else n + R; j + R; x + R; v + R; if c5 then n + L; j + L; x + L; v + L; stop else n + R; j + R; x + R; v + R; X}" +
    "def D{x&{l3: stop}}" +
    "def H{x&{R: stop, L: Q}}" +
    "def X{n&{l5: v + l6; D}}" +
    "main {n&{R: n&{l1: H}, L: if c2 then n + L; j + L; x + L; v + L; P else n + R; j + R; x + R; v + R; X}}" +
"}" +
"|" +
"v{" +
    "def P{n&{R: H, L: n!<m5>; x!<m6>; X}}" +
    "def Q{s&{R: s&{R: n!<m5>; x!<m6>; X, L: V}, L: n!<m5>; x!<m6>; X}}" +
    "def D{if c6 then V else V}" +
    "def V{x?; V}" +
    "def H{x&{R: V, L: Q}}" +
    "def X{s&{l6: n!<m3>; D}}" +
    "main {n&{R: H, L: s&{R: n!<m5>; x!<m6>; X, L: P}}}" +
"}" +
"|" +
"x{" +
    "def P{n&{R: H, L: X}}" +
    "def Q{n?; s&{R: s&{R: X, L: V}, L: X}}" +
    "def D{V}" +
    "def V{j&{l4: v!<m4>; V}}" +
    "def H{if c7 then n + L; j + L; v + L; s + L; Q else n + R; j + R; v + R; s + R; V}" +
    "def X{v?; s + l3; D}" +
    "main {n&{R: H, L: s&{R: X, L: P}}}" +
"}" +
"|" +
"j{" +
    "def P{n&{R: H, L: n&{l2: X}}}" +
    "def Q{s&{R: s&{R: X, L: V}, L: X}}" +
    "def D{V}" +
    "def V{x + l4; V}" +
    "def H{x&{R: if c8 then V else V, L: Q}}" +
    "def X{D}" +
    "main {n!<m1>; n&{R: H, L: s&{R: X, L: P}}}" +
"}" +
"|" +
"n{" +
    "def P{if c3 then j + L; x + L; v + L; s + L; j + l2; s + l5; v?; X else j + R; x + R; v + R; s + R; H}" +
    "def Q{s&{R: s&{R: s + l5; v?; X, L: stop}, L: s + l5; v?; X}}" +
    "def D{stop}" +
    "def H{x&{R: stop, L: x!<m2>; Q}}" +
    "def X{v?; D}" +
    "main {j?; if c1 then j + L; x + L; v + L; s + L; s&{R: s + l5; v?; X, L: P} else j + R; x + R; v + R; s + R; s + l1; H}" +
"}";
        var actual = Extraction.extractChoreography(test).toString();
        System.out.println(actual);
    }

    @Test
    void C109() {
        var test =
            "s{def P{v?; H} def Q{x?; n!<m5>; stop} def D{v?; X} def V{j&{R: v&{R: n?; X, L: v&{R: D, L: stop}}, L: v&{R: n?; X, L: v&{R: D, L: stop}}}} def H{v&{R: x&{R: V, L: H}, L: x&{R: x&{R: V, L: H}, L: x&{R: V, L: H}}}} def X{j + l4; Q} main {v&{R: if c2 then n + L; j + L; x + L; v + L; P else n + R; j + R; x + R; v + R; X, L: v + l1; P}}} | v{def P{n!<m1>; j?; s!<m3>; H} def D{n?; s!<m7>; stop} def V{j&{l3: if c3 then j&{R: n + L; j + L; x + L; s + L; if c5 then n + L; j + L; x + L; s + L; stop else n + R; j + R; x + R; s + R; D, L: n + L; j + L; x + L; s + L; if c5 then n + L; j + L; x + L; s + L; stop else n + R; j + R; x + R; s + R; D} else j&{R: n + R; j + R; x + R; s + R; stop, L: n + R; j + R; x + R; s + R; stop}}} def H{if c7 then n + L; j + L; x + L; s + L; x&{R: x&{R: V, L: H}, L: x&{R: V, L: H}} else n + R; j + R; x + R; s + R; x&{R: V, L: H}} main {if c1 then n + L; j + L; x + L; s + L; s&{l1: P} else n + R; j + R; x + R; s + R; s&{R: stop, L: P}}} | x{def P{H} def Q{s!<m4>; stop} def D{X} def V{j&{R: v&{R: X, L: v&{R: D, L: stop}}, L: v&{R: X, L: v&{R: D, L: stop}}}} def H{if c6 then v&{R: n + L; j + L; v + L; s + L; H, L: if c8 then n + L; j + L; v + L; s + L; n + L; j + L; v + L; s + L; H else n + R; j + R; v + R; s + R; n + L; j + L; v + L; s + L; H} else v&{R: n + R; j + R; v + R; s + R; V, L: if c8 then n + L; j + L; v + L; s + L; n + R; j + R; v + R; s + R; V else n + R; j + R; v + R; s + R; n + R; j + R; v + R; s + R; V}} def X{Q} main {v&{R: s&{R: X, L: P}, L: P}}} | j{def P{v!<m2>; H} def Q{n&{l2: stop}} def D{X} def V{v + l3; if c4 then n + L; x + L; v + L; s + L; v&{R: X, L: v&{R: D, L: stop}} else n + R; x + R; v + R; s + R; v&{R: X, L: v&{R: D, L: stop}}} def H{v&{R: x&{R: V, L: H}, L: x&{R: x&{R: V, L: H}, L: x&{R: V, L: H}}}} def X{s&{l4: Q}} main {v&{R: s&{R: X, L: P}, L: P}}} | n{def P{v?; H} def Q{j + l2; s?; stop} def D{v!<m6>; X} def V{j&{R: v&{R: s!<m8>; X, L: v&{R: D, L: stop}}, L: v&{R: s!<m8>; X, L: v&{R: D, L: stop}}}} def H{v&{R: x&{R: V, L: H}, L: x&{R: x&{R: V, L: H}, L: x&{R: V, L: H}}}} def X{Q} main {v&{R: s&{R: X, L: P}, L: P}}}";
        var actual = Extraction.extractChoreography(test).toString();
        System.out.println(actual);
    }

    @Test
    void C315_unrolled() {
        var test =
                "s{def P{x?; P} def Q{x?; P} def R{n?; V} def C{H} def D{j&{R: R, L: v&{R: D, L: X}}} def V{x&{R: H, L: x?; P}} def H{v&{R: v&{R: v&{R: n?; V, L: D}, L: j + l4; C}, L: v&{R: v&{R: n?; V, L: D}, L: j + l4; C}}} def X{if c8 then n + L; j + L; x + L; v + L; X else n + R; j + R; x + R; v + R; v + l6; Q} main {v&{R: Q, L: j + l4; C}}} | v{def Q{n?; stop} def R{V} def C{x!<m6>; H} def D{j&{R: x?; n!<m5>; R, L: if c3 then n + L; j + L; x + L; s + L; x?; X else n + R; j + R; x + R; s + R; D}} def V{x&{R: H, L: stop}} def H{if c5 then if c6 then n + L; j + L; x + L; s + L; n + L; j + L; x + L; s + L; C else n + R; j + R; x + R; s + R; n + L; j + L; x + L; s + L; C else if c6 then n + L; j + L; x + L; s + L; n + R; j + R; x + R; s + R; if c7 then n + L; j + L; x + L; s + L; D else n + R; j + R; x + R; s + R; V else n + R; j + R; x + R; s + R; n + R; j + R; x + R; s + R; if c7 then n + L; j + L; x + L; s + L; D else n + R; j + R; x + R; s + R; V} def X{s&{R: s&{l6: Q}, L: X}} main {if c1 then n + L; j + L; x + L; s + L; C else n + R; j + R; x + R; s + R; Q}} | x{def P{s!<m1>; P} def Q{s!<m1>; P} def R{v!<m4>; j&{l2: V}} def C{v?; H} def D{j&{R: R, L: v&{R: D, L: v!<m7>; X}}} def V{if c4 then n + L; j + L; v + L; s + L; s!<m1>; P else n + R; j + R; v + R; s + R; H} def H{v&{R: v&{R: v&{R: V, L: D}, L: n + l5; C}, L: v&{R: v&{R: V, L: D}, L: n + l5; C}}} def X{s&{R: Q, L: X}} main {v&{R: Q, L: n + l5; C}}} | j{def P{n + l1; P} def Q{n!<m3>; P} def R{V} def C{n + l3; s&{l4: H}} def D{if c2 then n + L; x + L; v + L; s + L; v&{R: D, L: X} else n + R; x + R; v + R; s + R; x + l2; R} def V{x&{R: H, L: P}} def H{v&{R: v&{R: v&{R: V, L: D}, L: C}, L: v&{R: v&{R: V, L: D}, L: C}}} def X{s&{R: Q, L: X}} main {v&{R: Q, L: C}}} | n{def P{j&{l1: P}} def Q{v!<m2>; j?; P} def R{v?; s!<m8>; V} def C{j&{l3: x&{l5: H}}} def D{j&{R: R, L: v&{R: D, L: X}}} def V{x&{R: H, L: P}} def H{v&{R: v&{R: v&{R: s!<m8>; V, L: D}, L: C}, L: v&{R: v&{R: s!<m8>; V, L: D}, L: C}}} def X{s&{R: Q, L: X}} main {v&{R: Q, L: C}}}";
        var actual = Extraction.extractChoreography(test).toString();
        System.out.println(actual);
    }
}