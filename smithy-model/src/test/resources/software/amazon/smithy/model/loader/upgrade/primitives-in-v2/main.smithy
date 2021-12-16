$version: "2.0"

namespace smithy.example

// These Primitive* targets are not allowed in v2.
structure Bad {
    boolean: PrimitiveBoolean,
    byte: PrimitiveByte,
    short: PrimitiveShort,
    integer: PrimitiveInteger,
    long: PrimitiveLong,
    float: PrimitiveFloat,
    double: PrimitiveDouble
}
