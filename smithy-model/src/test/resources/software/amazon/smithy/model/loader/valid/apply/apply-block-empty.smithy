$version: "2.0"
namespace smithy.example

structure Foo {
    baz: String,
}

// Block apply statements may be empty.
apply Foo$baz {}
