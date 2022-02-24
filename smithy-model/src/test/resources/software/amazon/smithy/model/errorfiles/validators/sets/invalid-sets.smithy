$version: "1.0"

namespace smithy.example

set TimestampSet {
    member: Timestamp
}

set BooleanSet {
    member: Boolean
}

set DoubleSet {
    member: Double
}

set FloatSet {
    member: Float
}

set DocumentSet {
    member: Document
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

set StringSet {
    member: String
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

set StreamingBlobSet {
    member: StreamingBlob
}

@streaming
blob StreamingBlob

set StreamingStringSet {
    member: StreamingString
}

@streaming
string StreamingString
