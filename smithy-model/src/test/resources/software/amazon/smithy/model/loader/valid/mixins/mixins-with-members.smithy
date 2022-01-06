$version: "2.0"

namespace smithy.example

@mixin
structure A {
    a: String
}

@mixin
structure B with [A] {
    b: String
}

@mixin
structure C {
    c: String
}

@mixin
structure D with [C] {
    d: String
}

@mixin
structure E with [D] {
    e: String
}

structure F with [B, E] {
    f: String
}
