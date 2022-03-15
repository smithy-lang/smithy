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

    handlesComments: // Nobody actually does this right?
        PrimitiveShort,

    @default
    handlesPreexistingDefault: PrimitiveShort,

    @required
    handlesRequired: PrimitiveLong,

    @box
    handlesBox: PrimitiveByte,
}
