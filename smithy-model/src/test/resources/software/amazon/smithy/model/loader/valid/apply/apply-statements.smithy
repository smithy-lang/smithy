$version: "2.0"
namespace smithy.example

structure Foo {
    baz: String,
}

apply Foo$baz @documentation("Hi")
