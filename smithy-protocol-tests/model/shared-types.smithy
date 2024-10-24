// This file contains shared types that are used throughout the test cases.
//
// Anything that is generic enough that it could potentially be reused
// should be defined in this file. However, things like input or output
// structures or other test-case specific shapes should be defined closer to
// the test case and in its same file.

$version: "2.0"

metadata validators = [
    {
        name: "EmitEachSelector"
        id: "UnboundTestOperation"
        severity: "WARNING"
        message: "This operation in the Smithy protocol tests is not bound to a service."
        namespaces: [
            // Overall protocol test suites.
            "smithy.protocoltests.rpcv2Cbor"
        ]
        configuration: {
            "selector": "operation :not(< service)"
        }
    }
]

namespace smithy.protocoltests.shared

list StringList {
    member: String,
}

@sparse
list SparseStringList {
    member: String
}

@uniqueItems
list StringSet {
    member: String,
}

map StringMap {
    key: String,
    value: String,
}

map StringListMap {
    key: String,
    value: StringList
}

@sparse
map SparseStringMap {
    key: String,
    value: String,
}

/// A list of lists of strings.
list NestedStringList {
    member: StringList,
}

list ShortList {
    member: Short,
}

list IntegerList {
    member: Integer,
}

@uniqueItems
list IntegerSet {
    member: Integer,
}

list FloatList {
    member: Float,
}

list DoubleList {
    member: Double,
}

list BooleanList {
    member: Boolean,
}

@uniqueItems
list BooleanSet {
    member: Boolean,
}

list TimestampList {
    member: Timestamp,
}

list BlobList {
    member: Blob,
}

@uniqueItems
list BlobSet {
    member: Blob,
}

list ByteList {
    member: Byte,
}

@uniqueItems
list ByteSet {
    member: Byte,
}
@uniqueItems
list ShortSet {
    member: Short,
}

@uniqueItems
list LongList {
    member: Long,
}

@uniqueItems
list LongSet {
    member: Long,
}

@uniqueItems
list TimestampSet {
    member: Timestamp,
}

list DateTimeList {
    member: DateTime,
}

@uniqueItems
list DateTimeSet {
    member: DateTime,
}

@uniqueItems
list HttpDateSet {
    member: HttpDate,
}

@uniqueItems
list ListSet {
    member: StringList
}

@uniqueItems
list StructureSet {
    member: GreetingStruct
}

@uniqueItems
list UnionSet {
    member: FooUnion
}

union FooUnion {
    string: String
    integer: Integer
}

enum FooEnum {
    FOO = "Foo"
    BAZ = "Baz"
    BAR = "Bar"
    ONE = "1"
    ZERO = "0"
}

list FooEnumList {
    member: FooEnum,
}

@uniqueItems
list FooEnumSet {
    member: FooEnum,
}

map FooEnumMap {
    key: String,
    value: FooEnum,
}

@timestampFormat("date-time")
timestamp DateTime

@timestampFormat("epoch-seconds")
timestamp EpochSeconds

@timestampFormat("http-date")
timestamp HttpDate

@mediaType("text/plain")
blob TextPlainBlob

@mediaType("image/jpeg")
blob JpegBlob

structure GreetingStruct {
    hi: String
}

list GreetingList {
    member: GreetingStruct
}

intEnum IntegerEnum {
    A = 1
    B = 2
    C = 3
}

list IntegerEnumList {
    member: IntegerEnum
}

@uniqueItems
list IntegerEnumSet {
    member: IntegerEnum
}

map IntegerEnumMap {
    key: String,
    value: IntegerEnum
}
