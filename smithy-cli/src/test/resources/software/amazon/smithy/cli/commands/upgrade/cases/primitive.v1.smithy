$version: "1.0"

namespace com.example

structure PrimitiveBearer {
    int: PrimitiveInteger,
    bool: PrimitiveBoolean,
    byte: PrimitiveByte,
    double: PrimitiveDouble,
    float: PrimitiveFloat,
    long: PrimitiveLong,
    short: PrimitiveShort,

    handlesComments: PrimitiveShort, // comment

    @required
    handlesRequired: PrimitiveLong,

    @box
    handlesBox: PrimitiveByte,
}
