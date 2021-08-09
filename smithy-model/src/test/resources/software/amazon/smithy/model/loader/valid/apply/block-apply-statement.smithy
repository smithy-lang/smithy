$version: "1.1"
namespace smithy.example

structure Foo {
    baz: String,
}

apply Foo$baz {
    @documentation("Hi")
    @sensitive
    @deprecated
}
