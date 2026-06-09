// Regression test for MemberShouldReferenceResource: the validator used to call
// PathFinder.search (which enumerates EVERY simple path) once per resource. On a
// densely connected, recursive shape graph the number of simple paths is factorial,
// so validation effectively hangs. N0..N14 form a near-complete digraph (each
// structure references every other), disconnected from FooResource. Validating N1$id
// used to enumerate the ~e*14! simple paths through that graph; it must now complete
// quickly and still flag the unbound identifier member while leaving the
// resource-bound GetFooInput$id alone.
$version: "2.0"

namespace ns.recursive

resource FooResource {
    identifiers: {
        id: String
    }
    read: GetFoo
}

@readonly
operation GetFoo {
    input := {
        // Bound to FooResource through its `read` lifecycle, so this matching
        // identifier member must be ignored (it is reachable from the resource).
        @required
        id: String
    }
    output := {}
}

// Near-complete, recursive digraph disconnected from any resource. N1 carries an
// identifier member that is NOT bound to a resource and should therefore be flagged.
structure N0 {
    m1: N1
    m2: N2
    m3: N3
    m4: N4
    m5: N5
    m6: N6
    m7: N7
    m8: N8
    m9: N9
    m10: N10
    m11: N11
    m12: N12
    m13: N13
    m14: N14
}

structure N1 {
    id: String
    m0: N0
    m2: N2
    m3: N3
    m4: N4
    m5: N5
    m6: N6
    m7: N7
    m8: N8
    m9: N9
    m10: N10
    m11: N11
    m12: N12
    m13: N13
    m14: N14
}

structure N2 {
    m0: N0
    m1: N1
    m3: N3
    m4: N4
    m5: N5
    m6: N6
    m7: N7
    m8: N8
    m9: N9
    m10: N10
    m11: N11
    m12: N12
    m13: N13
    m14: N14
}

structure N3 {
    m0: N0
    m1: N1
    m2: N2
    m4: N4
    m5: N5
    m6: N6
    m7: N7
    m8: N8
    m9: N9
    m10: N10
    m11: N11
    m12: N12
    m13: N13
    m14: N14
}

structure N4 {
    m0: N0
    m1: N1
    m2: N2
    m3: N3
    m5: N5
    m6: N6
    m7: N7
    m8: N8
    m9: N9
    m10: N10
    m11: N11
    m12: N12
    m13: N13
    m14: N14
}

structure N5 {
    m0: N0
    m1: N1
    m2: N2
    m3: N3
    m4: N4
    m6: N6
    m7: N7
    m8: N8
    m9: N9
    m10: N10
    m11: N11
    m12: N12
    m13: N13
    m14: N14
}

structure N6 {
    m0: N0
    m1: N1
    m2: N2
    m3: N3
    m4: N4
    m5: N5
    m7: N7
    m8: N8
    m9: N9
    m10: N10
    m11: N11
    m12: N12
    m13: N13
    m14: N14
}

structure N7 {
    m0: N0
    m1: N1
    m2: N2
    m3: N3
    m4: N4
    m5: N5
    m6: N6
    m8: N8
    m9: N9
    m10: N10
    m11: N11
    m12: N12
    m13: N13
    m14: N14
}

structure N8 {
    m0: N0
    m1: N1
    m2: N2
    m3: N3
    m4: N4
    m5: N5
    m6: N6
    m7: N7
    m9: N9
    m10: N10
    m11: N11
    m12: N12
    m13: N13
    m14: N14
}

structure N9 {
    m0: N0
    m1: N1
    m2: N2
    m3: N3
    m4: N4
    m5: N5
    m6: N6
    m7: N7
    m8: N8
    m10: N10
    m11: N11
    m12: N12
    m13: N13
    m14: N14
}

structure N10 {
    m0: N0
    m1: N1
    m2: N2
    m3: N3
    m4: N4
    m5: N5
    m6: N6
    m7: N7
    m8: N8
    m9: N9
    m11: N11
    m12: N12
    m13: N13
    m14: N14
}

structure N11 {
    m0: N0
    m1: N1
    m2: N2
    m3: N3
    m4: N4
    m5: N5
    m6: N6
    m7: N7
    m8: N8
    m9: N9
    m10: N10
    m12: N12
    m13: N13
    m14: N14
}

structure N12 {
    m0: N0
    m1: N1
    m2: N2
    m3: N3
    m4: N4
    m5: N5
    m6: N6
    m7: N7
    m8: N8
    m9: N9
    m10: N10
    m11: N11
    m13: N13
    m14: N14
}

structure N13 {
    m0: N0
    m1: N1
    m2: N2
    m3: N3
    m4: N4
    m5: N5
    m6: N6
    m7: N7
    m8: N8
    m9: N9
    m10: N10
    m11: N11
    m12: N12
    m14: N14
}

structure N14 {
    m0: N0
    m1: N1
    m2: N2
    m3: N3
    m4: N4
    m5: N5
    m6: N6
    m7: N7
    m8: N8
    m9: N9
    m10: N10
    m11: N11
    m12: N12
    m13: N13
}
