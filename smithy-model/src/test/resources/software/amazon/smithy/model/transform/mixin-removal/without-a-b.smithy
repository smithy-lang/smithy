$version: "2.0"

namespace smithy.example

@mixin
structure A2 {
    a2: String
}

@mixin
structure A3 with [A2] {
    a3: String
}

@mixin
structure B2 {
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
