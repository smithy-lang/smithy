$version: "2.0"

namespace smithy.example

/// A
@mixin
@sensitive
structure A {
    /// A.a
    a: String
}

/// B
@deprecated
@mixin
structure B with [A] {
    /// B.b
    b: String
}

/// C
@mixin
@tags([
    "a"
])
structure C {
    /// C.c
    c: String
}

/// D
@externalDocumentation(
    web: "http://example.com"
)
@mixin
structure D with [C] {
    /// D.d
    d: String
}

apply D$c {
    @documentation("I've changed")
    @internal
}

/// E
@mixin
@since("X")
structure E with [D] {
    /// E.e
    e: String
}

/// F
@internal
structure F with [
    B
    E
] {
    /// F.f
    f: String
}

apply F$a {
    @documentation("I've changed")
    @internal
}

apply F$e @internal

structure G with [
    B
    E
] {}

structure H with [B] {}

structure I {}
