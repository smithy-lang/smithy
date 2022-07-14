$version: "1.0"

namespace smithy.example

set StringSet {
    member: String
}

set BlobSet {
    member: Blob
}

set ByteSet {
    member: Byte
}

set ShortSet {
    member: Short
}

set IntegerSet {
    member: Integer
}

set LongSet {
    member: Long
}

set BigIntSet {
    member: BigInteger
}

set BigDecimalSet {
    member: BigDecimal
}

set TimestampSet {
    member: Timestamp
}

set BooleanSet {
    member: Boolean
}

set ListSet {
    member: StringList
}

list StringList {
    member: String
}

set SetSet {
    member: StringSet
}

set MapSet {
    member: StringMap
}

map StringMap {
    key: String,
    value: String
}

set StructSet {
    member: StructureExample
}

structure StructureExample {}

set UnionSet {
    member: UnionExample
}

union UnionExample {
    foo: String
}
