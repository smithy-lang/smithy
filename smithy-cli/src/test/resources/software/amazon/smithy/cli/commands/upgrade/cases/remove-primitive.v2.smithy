$version: "2.0"

namespace com.example

structure PrimitiveBearer {
    @default(0)
    int: Integer,
    @default(false)
    bool: Boolean,
    @default(0)
    byte: Byte,
    @default(0)
    double: Double,
    @default(0)
    float: Float,
    @default(0)
    long: Long,
    @default(0)
    short: Short,

    @default(0)
    handlesComments: // Nobody actually does this right?
        Short,

    @default(0)
    handlesPreexistingDefault: Short,

    @required
    handlesRequired: Long,

    handlesBox: Byte,
}
