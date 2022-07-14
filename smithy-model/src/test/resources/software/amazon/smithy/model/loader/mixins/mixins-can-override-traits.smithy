$version: "2"
namespace smithy.example

/// A
@sensitive
@mixin
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
@tags(["a"])
structure C {
    /// C.c
    c: String
}

/// D
@mixin
@externalDocumentation(web: "http://example.com")
structure D with [C] {
    /// D.d
    d: String
}

// Override traits on the inherited member.
apply D$c @internal
apply D$c @documentation("I've changed")

/// E
@since("X")
@mixin
structure E with [D] {
    /// E.e
    e: String
}

/// F
@internal
structure F with [B, E] {
    /// F.f
    f: String
}

// Override the docs of a on F
apply F$a @documentation("I've changed")
