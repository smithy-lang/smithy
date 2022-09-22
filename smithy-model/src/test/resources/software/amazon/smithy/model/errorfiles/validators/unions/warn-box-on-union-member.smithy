$version: "1.0"

namespace smithy.example

union Example {
    @box
    a: PrimitiveInteger,

    b: Integer
}
