$version: "2.0"

namespace smithy.example

structure Foo {
    @default(0)
    number: PrimitiveInteger
}

structure Baz {
    @default(0)
    number: Integer
}
