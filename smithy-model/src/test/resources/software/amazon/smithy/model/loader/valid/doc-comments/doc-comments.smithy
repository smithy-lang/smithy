$version: "2.0"

namespace smithy.example

// The previous doc comment does not affect this shape,
// and this shape is not documented.
string NotDocumented

/// Foo
/// baz
/// Bar!
string MyString

/// Structure
structure MyStructure {
    /// Docs on member!
    foo: String = "hi",

    /// Docs on another member!
    @required
    baz: String,

    // no docs.
    bam: String,
}

/// The documentation
/// of this trait!
@trait
structure MyTrait {
    /// These are treated as comments.
    /// more...
}

operation MyOperation {
    input := {}

    output :=
        /// These are valid because they come AFTER the walrus
        /// operator.
        {}
}
