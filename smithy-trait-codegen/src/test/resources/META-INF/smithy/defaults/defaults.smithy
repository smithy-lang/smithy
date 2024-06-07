$version: "2"

namespace test.smithy.traitcodegen.defaults

@trait
structure StructDefaults {
    @default([])
    defaultList: StringList

    @default({})
    defaultMap: StringMap

    @default(true)
    defaultBoolean: Boolean

    @default("default")
    defaultString: String

    @default(1)
    defaultByte: Byte

    @default(1)
    defaultShort: Short

    @default(1)
    defaultInt: Integer

    @default(1)
    defaultLong: Long

    @default(2.2)
    defaultFloat: Float

    @default(1.1)
    defaultDouble: Double

    @default(100)
    defaultBigInt: BigInteger

    @default(100.01)
    defaultBigDecimal: BigDecimal

    @default("1985-04-12T23:20:50.52Z")
    defaultTimestamp: Timestamp
}

@private
list StringList {
    member: String
}

@private
map StringMap {
    key: String
    value: String
}
