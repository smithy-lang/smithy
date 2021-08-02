$version: "1.1"

namespace smithy.example

/// F
@internal
@deprecated
@externalDocumentation(web: "http://example.com")
@sensitive
@since("X")
@tags(["a"])
structure F {
    /// I've changed
    a: String,

    /// B.b
    b: String,

    /// I've changed
    @sensitive
    c: String,

    /// D.d
    d: String,

    /// E.e
    e: String,

    /// F.f
    f: String,
}
