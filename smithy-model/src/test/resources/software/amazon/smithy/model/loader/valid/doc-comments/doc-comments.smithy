/// This comment is ignored.
$version: "2.0"

/// This comment is ignored.
metadata foo = [
    /// This comment is ignored.
    "bar"
]

/// This comment is ignored.
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
    /// IMPORTANT: These docs are ignored since they come after traits!
    baz: String,

    // no docs.
    bam: String,
}

/// The documentation
/// of this trait!
@trait
/// IMPORTANT: These docs are ignored since they come after traits!
structure MyTrait {
    /// These are treated as comments.
    /// more...
}

operation MyOperation {
    input
        /// These docs are ignored because they come BEFORE the walrus
        /// operator.
        := {}

    output :=
        /// These are not ignored because they come AFTER the walrus
        /// operator.
        {}
}

/// This is treated as a comment because it comes before apply.
apply MyOperation @deprecated

/// This is treated as a comment.
