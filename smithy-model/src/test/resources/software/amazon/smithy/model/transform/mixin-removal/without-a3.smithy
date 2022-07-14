$version: "2.0"

namespace smithy.example

@mixin
structure A {
    /// A
    a: String
}

@mixin
structure A2 with [A] {
    a2: String
}

apply A2$a @documentation("A2")

@mixin
structure B {
    b: String
}

@mixin
structure B2 with [B] {
    b2: String
}

@mixin
structure B3 with [B2] {
    b3: String
}

@mixin
structure C with [B3] {
    c: String
}

structure D with [C] {
    d: String
}
