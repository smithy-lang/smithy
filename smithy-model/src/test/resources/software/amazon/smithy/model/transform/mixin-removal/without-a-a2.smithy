$version: "2.0"

namespace smithy.example

@mixin
structure A3 {
    a3: String
}

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
structure C with [A3, B3] {
    c: String
}

structure D with [C] {
    d: String
}
