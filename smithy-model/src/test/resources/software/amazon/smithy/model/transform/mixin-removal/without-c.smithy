// Removing "C" will update D such that D has no mixins, D no longer has
// a2, a3, b, b2, or b3. D still has a because a trait was added to the
// inherited a member of D to update the documentation to "D".
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

structure D {
    d: String
}
