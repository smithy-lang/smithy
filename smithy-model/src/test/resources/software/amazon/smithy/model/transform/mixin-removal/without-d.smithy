// Removing "D" has no effect on other shapes, even shapes that are mixins
// that D used. Those shapes aren't dependent on D.
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
structure A3 with [A2] {
    a3: String
}

apply A3$a @documentation("A3")

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

apply C$a @documentation("C")
