$version: "2.0"

namespace com.example

structure PrimitiveBearer {
    @default
    int: Integer,
    @default
    bool: Boolean,
    @default
    byte: Byte,
    @default
    double: Double,
    @default
    float: Float,
    @default
    long: Long,
    @default
    short: Short,

    @default
    handlesComments: // Nobody actually does this right?
        Short,

    @default
    handlesPreexistingDefault: Short,

    @required
    handlesRequired: Long,

    handlesBox: Byte,
}
