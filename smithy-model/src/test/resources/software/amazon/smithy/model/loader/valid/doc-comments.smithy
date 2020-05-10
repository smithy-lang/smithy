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
    foo: String,

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

/// This is treated as a comment.
