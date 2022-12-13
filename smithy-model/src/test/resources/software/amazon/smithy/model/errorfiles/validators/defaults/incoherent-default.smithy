$version: "2.0"

namespace smithy.example

structure Foo {
    bad1: PrimitiveInteger = 1      // must be 0
    bad2: PrimitiveInteger          // missing default
    okExplicitNull1: Integer = null // this is ok
    okValue: Integer = 1            // this is ok
}
