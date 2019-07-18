namespace smithy.example

/// Foo
/// baz
/// Bar!
string MyString

/// Structure
structure MyStructure {
    /// Docs on member!
    foo: String,

    /// Docs on another member!
    baz: String,
}

/// The documentation
/// of this trait!
@trait
structure MyTrait {}
