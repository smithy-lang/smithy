// This file is actually meaningless because main.smithy contains errors.
$version: "2.0"

namespace smithy.example

structure Bad {
    boolean: PrimitiveBoolean,
    byte: PrimitiveByte,
    short: PrimitiveShort,
    integer: PrimitiveInteger,
    long: PrimitiveLong,
    float: PrimitiveFloat,
    double: PrimitiveDouble
}
