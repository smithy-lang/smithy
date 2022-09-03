$version: "2.0"

namespace smithy.example

structure Foo {
    integer: PrimitiveInteger = null
    explicitNull: Integer = null
}

@default(null) // invalid default trait
integer NullableInteger
