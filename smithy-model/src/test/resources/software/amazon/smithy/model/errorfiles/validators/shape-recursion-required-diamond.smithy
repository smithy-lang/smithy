$version: "2.0"

namespace smithy.example

// Every structure here has more than one recursion path. Each recursive
// structure is reported once.
structure S0 {
    @required
    a: S1

    @required
    b: S1
}

structure S1 {
    @required
    a: S2

    @required
    b: S2
}

structure S2 {
    @required
    a: S3

    @required
    b: S3
}

structure S3 {
    @required
    a: S4

    @required
    b: S4
}

structure S4 {
    @required
    back: S0
}
