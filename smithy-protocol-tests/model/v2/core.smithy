$version: "2.0"

namespace smithy.protocoltests.corpus

// CoreProtocolTestService exercises the fundamental state transitions between
// different shape types within (de)serialization.
//
// A deserializer is a state machine: at any point it's inside a container
// (struct, list, map, union) and reads the next element, which is a scalar or
// another container. Every container x element pair is a transition, and this
// layer covers all 20 of them, plus sparse variants, errors, and the empty and
// absent body edge cases. Every protocol mixes this in.
//
// Each operation tests all scalar types at once so the "matrix" stays linear
// instead of scalar x container.
//
// The Document type is _excluded_ from this corpus due to its limited support
// across protocols, it is covered separately.
@mixin
service CoreProtocolTestService {
    operations: [
        ScalarMembers
        ListOfScalars
        MapOfScalars
        UnionOfScalars
        UnionOfStruct
        UnionOfList
        UnionOfMap
        UnionOfUnion
        StructOfScalars
        ListOfStructs
        ListOfMaps
        ListOfLists
        ListOfUnions
        MapOfStructs
        MapOfMaps
        MapOfLists
        MapOfUnions
        SparseListOfScalars
        SparseMapOfScalars
        SparseListOfStructs
        SparseMapOfStructs
        RecursiveStruct
        RecursiveUnion
        EmptyInputOutput
        NoInputOutput
        ErrorOperation
    ]
}

operation ScalarMembers {
    input: ScalarMembersInputOutput
    output: ScalarMembersInputOutput
}

structure ScalarMembersInputOutput {
    booleanMember: Boolean
    byteMember: Byte
    shortMember: Short
    integerMember: Integer
    longMember: Long
    floatMember: Float
    doubleMember: Double
    bigIntegerMember: BigInteger
    bigDecimalMember: BigDecimal
    stringMember: String
    blobMember: Blob
    dateTimeMember: DateTimeTimestamp
    epochSecondsMember: EpochSecondsTimestamp
    httpDateMember: HttpDateTimestamp
    stringEnum: CorpusStringEnum
    intEnum: CorpusIntEnum
}

operation ListOfScalars {
    input: ListOfScalarsInputOutput
    output: ListOfScalarsInputOutput
}

structure ListOfScalarsInputOutput {
    booleans: BooleanList
    bytes: ByteList
    shorts: ShortList
    integers: IntegerList
    longs: LongList
    floats: FloatList
    doubles: DoubleList
    bigIntegers: BigIntegerList
    bigDecimals: BigDecimalList
    strings: StringList
    blobs: BlobList
    timestamps: TimestampList
    enums: CorpusStringEnumList
    intEnums: CorpusIntEnumList
}

operation SparseListOfScalars {
    input: SparseListOfScalarsInputOutput
    output: SparseListOfScalarsInputOutput
}

structure SparseListOfScalarsInputOutput {
    booleans: SparseBooleanList
    bytes: SparseByteList
    shorts: SparseShortList
    integers: SparseIntegerList
    longs: SparseLongList
    floats: SparseFloatList
    doubles: SparseDoubleList
    bigIntegers: SparseBigIntegerList
    bigDecimals: SparseBigDecimalList
    strings: SparseStringList
    blobs: SparseBlobList
    timestamps: SparseTimestampList
    enums: SparseCorpusStringEnumList
    intEnums: SparseCorpusIntEnumList
}

operation MapOfScalars {
    input: MapOfScalarsInputOutput
    output: MapOfScalarsInputOutput
}

structure MapOfScalarsInputOutput {
    booleans: BooleanMap
    bytes: ByteMap
    shorts: ShortMap
    integers: IntegerMap
    longs: LongMap
    floats: FloatMap
    doubles: DoubleMap
    bigIntegers: BigIntegerMap
    bigDecimals: BigDecimalMap
    strings: StringMap
    blobs: BlobMap
    timestamps: TimestampMap
    enums: CorpusStringEnumMap
    intEnums: CorpusIntEnumMap
}

operation SparseMapOfScalars {
    input: SparseMapOfScalarsInputOutput
    output: SparseMapOfScalarsInputOutput
}

structure SparseMapOfScalarsInputOutput {
    booleans: SparseBooleanMap
    bytes: SparseByteMap
    shorts: SparseShortMap
    integers: SparseIntegerMap
    longs: SparseLongMap
    floats: SparseFloatMap
    doubles: SparseDoubleMap
    bigIntegers: SparseBigIntegerMap
    bigDecimals: SparseBigDecimalMap
    strings: SparseStringMap
    blobs: SparseBlobMap
    timestamps: SparseTimestampMap
    enums: SparseCorpusStringEnumMap
    intEnums: SparseCorpusIntEnumMap
}

operation UnionOfScalars {
    input: UnionOfScalarsInputOutput
    output: UnionOfScalarsInputOutput
}

structure UnionOfScalarsInputOutput {
    value: CorpusUnion
}

operation UnionOfStruct {
    input: UnionOfStructInputOutput
    output: UnionOfStructInputOutput
}

structure UnionOfStructInputOutput {
    value: CorpusUnion
}

operation UnionOfList {
    input: UnionOfListInputOutput
    output: UnionOfListInputOutput
}

structure UnionOfListInputOutput {
    value: CorpusUnion
}

operation UnionOfMap {
    input: UnionOfMapInputOutput
    output: UnionOfMapInputOutput
}

structure UnionOfMapInputOutput {
    value: CorpusUnion
}

operation UnionOfUnion {
    input: UnionOfUnionInputOutput
    output: UnionOfUnionInputOutput
}

structure UnionOfUnionInputOutput {
    value: CorpusUnion
}

union CorpusUnion {
    booleanValue: Boolean
    byteValue: Byte
    shortValue: Short
    integerValue: Integer
    longValue: Long
    floatValue: Float
    doubleValue: Double
    bigIntegerValue: BigInteger
    bigDecimalValue: BigDecimal
    stringValue: String
    blobValue: Blob
    timestampValue: NoFormatTimestamp
    enumValue: CorpusStringEnum
    intEnumValue: CorpusIntEnum
    listValue: StringList
    mapValue: StringMap
    structValue: SimpleStruct
    unionValue: CorpusSubUnion
}

union CorpusSubUnion {
    stringValue: String
    integerValue: Integer
}

operation StructOfScalars {
    input: StructOfScalarsInputOutput
    output: StructOfScalarsInputOutput
}

structure StructOfScalarsInputOutput {
    value: ScalarStruct
}

structure ScalarStruct {
    booleanMember: Boolean
    byteMember: Byte
    shortMember: Short
    integerMember: Integer
    longMember: Long
    floatMember: Float
    doubleMember: Double
    bigIntegerMember: BigInteger
    bigDecimalMember: BigDecimal
    stringMember: String
    blobMember: Blob
    dateTimeMember: DateTimeTimestamp
    epochSecondsMember: EpochSecondsTimestamp
    httpDateMember: HttpDateTimestamp
    stringEnum: CorpusStringEnum
    intEnum: CorpusIntEnum
}

operation ListOfStructs {
    input: ListOfStructsInputOutput
    output: ListOfStructsInputOutput
}

structure ListOfStructsInputOutput {
    values: SimpleStructList
}

operation ListOfMaps {
    input: ListOfMapsInputOutput
    output: ListOfMapsInputOutput
}

structure ListOfMapsInputOutput {
    booleans: ListOfBooleanMap
    integers: ListOfIntegerMap
    strings: ListOfStringMap
    blobs: ListOfBlobMap
    timestamps: ListOfTimestampMap
    enums: ListOfCorpusStringEnumMap
    intEnums: ListOfCorpusIntEnumMap
}

operation ListOfLists {
    input: ListOfListsInputOutput
    output: ListOfListsInputOutput
}

structure ListOfListsInputOutput {
    booleans: ListOfBooleanList
    integers: ListOfIntegerList
    strings: ListOfStringList
    blobs: ListOfBlobList
    timestamps: ListOfTimestampList
    enums: ListOfCorpusStringEnumList
    intEnums: ListOfCorpusIntEnumList
}

operation ListOfUnions {
    input: ListOfUnionsInputOutput
    output: ListOfUnionsInputOutput
}

structure ListOfUnionsInputOutput {
    values: CorpusUnionList
}

operation MapOfStructs {
    input: MapOfStructsInputOutput
    output: MapOfStructsInputOutput
}

structure MapOfStructsInputOutput {
    values: SimpleStructMap
}

operation MapOfMaps {
    input: MapOfMapsInputOutput
    output: MapOfMapsInputOutput
}

structure MapOfMapsInputOutput {
    booleans: MapOfBooleanMap
    integers: MapOfIntegerMap
    strings: MapOfStringMap
    blobs: MapOfBlobMap
    timestamps: MapOfTimestampMap
    enums: MapOfCorpusStringEnumMap
    intEnums: MapOfCorpusIntEnumMap
}

operation MapOfLists {
    input: MapOfListsInputOutput
    output: MapOfListsInputOutput
}

structure MapOfListsInputOutput {
    booleans: MapOfBooleanList
    integers: MapOfIntegerList
    strings: MapOfStringList
    blobs: MapOfBlobList
    timestamps: MapOfTimestampList
    enums: MapOfCorpusStringEnumList
    intEnums: MapOfCorpusIntEnumList
}

operation MapOfUnions {
    input: MapOfUnionsInputOutput
    output: MapOfUnionsInputOutput
}

structure MapOfUnionsInputOutput {
    values: CorpusUnionMap
}

operation SparseListOfStructs {
    input: SparseListOfStructsInputOutput
    output: SparseListOfStructsInputOutput
}

structure SparseListOfStructsInputOutput {
    values: SparseSimpleStructList
}

operation SparseMapOfStructs {
    input: SparseMapOfStructsInputOutput
    output: SparseMapOfStructsInputOutput
}

structure SparseMapOfStructsInputOutput {
    values: SparseSimpleStructMap
}

operation RecursiveStruct {
    input: RecursiveStructInputOutput
    output: RecursiveStructInputOutput
}

structure RecursiveStructInputOutput {
    value: RecursiveStructShape
}

structure RecursiveStructShape {
    stringMember: String
    recursiveMember: RecursiveStructShape
    recursiveList: RecursiveStructList
    recursiveMap: RecursiveStructMap
}

list RecursiveStructList {
    member: RecursiveStructShape
}

map RecursiveStructMap {
    key: String
    value: RecursiveStructShape
}

operation RecursiveUnion {
    input: RecursiveUnionInputOutput
    output: RecursiveUnionInputOutput
}

structure RecursiveUnionInputOutput {
    value: RecursiveUnionShape
}

union RecursiveUnionShape {
    stringValue: String
    recursiveValue: RecursiveUnionShape
    structValue: RecursiveUnionStruct
}

structure RecursiveUnionStruct {
    value: RecursiveUnionShape
}

operation EmptyInputOutput {
    input: EmptyInputOutputInput
    output: EmptyInputOutputOutput
}

structure EmptyInputOutputInput {}

structure EmptyInputOutputOutput {}

operation NoInputOutput {}

operation ErrorOperation {
    input: ErrorOperationInput
    output: ErrorOperationOutput
    errors: [
        SimpleError
        ComplexError
    ]
}

structure ErrorOperationInput {}

structure ErrorOperationOutput {}

@error("client")
structure SimpleError {
    message: String
}

@error("server")
structure ComplexError {
    message: String
    code: Integer
    nested: ComplexNestedError
}

structure ComplexNestedError {
    stringMember: String
    integerMember: Integer
}

structure SimpleStruct {
    stringMember: String
    integerMember: Integer
    booleanMember: Boolean
}

list BooleanList {
    member: Boolean
}

list ByteList {
    member: Byte
}

list ShortList {
    member: Short
}

list IntegerList {
    member: Integer
}

list LongList {
    member: Long
}

list FloatList {
    member: Float
}

list DoubleList {
    member: Double
}

list BigIntegerList {
    member: BigInteger
}

list BigDecimalList {
    member: BigDecimal
}

list StringList {
    member: String
}

list BlobList {
    member: Blob
}

list TimestampList {
    member: NoFormatTimestamp
}

list DateTimeTimestampList {
    member: DateTimeTimestamp
}

list HttpDateTimestampList {
    member: HttpDateTimestamp
}

timestamp NoFormatTimestamp

@timestampFormat("date-time")
timestamp DateTimeTimestamp

@timestampFormat("epoch-seconds")
timestamp EpochSecondsTimestamp

@timestampFormat("http-date")
timestamp HttpDateTimestamp

list CorpusStringEnumList {
    member: CorpusStringEnum
}

list CorpusIntEnumList {
    member: CorpusIntEnum
}

list SimpleStructList {
    member: SimpleStruct
}

list ListOfBooleanList {
    member: BooleanList
}

list ListOfIntegerList {
    member: IntegerList
}

list ListOfStringList {
    member: StringList
}

list ListOfBlobList {
    member: BlobList
}

list ListOfTimestampList {
    member: TimestampList
}

list ListOfCorpusStringEnumList {
    member: CorpusStringEnumList
}

list ListOfCorpusIntEnumList {
    member: CorpusIntEnumList
}

list ListOfBooleanMap {
    member: BooleanMap
}

list ListOfIntegerMap {
    member: IntegerMap
}

list ListOfStringMap {
    member: StringMap
}

list ListOfBlobMap {
    member: BlobMap
}

list ListOfTimestampMap {
    member: TimestampMap
}

list ListOfCorpusStringEnumMap {
    member: CorpusStringEnumMap
}

list ListOfCorpusIntEnumMap {
    member: CorpusIntEnumMap
}

list CorpusUnionList {
    member: CorpusUnion
}

@sparse
list SparseBooleanList {
    member: Boolean
}

@sparse
list SparseByteList {
    member: Byte
}

@sparse
list SparseShortList {
    member: Short
}

@sparse
list SparseIntegerList {
    member: Integer
}

@sparse
list SparseLongList {
    member: Long
}

@sparse
list SparseFloatList {
    member: Float
}

@sparse
list SparseDoubleList {
    member: Double
}

@sparse
list SparseBigIntegerList {
    member: BigInteger
}

@sparse
list SparseBigDecimalList {
    member: BigDecimal
}

@sparse
list SparseStringList {
    member: String
}

@sparse
list SparseBlobList {
    member: Blob
}

@sparse
list SparseTimestampList {
    member: NoFormatTimestamp
}

@sparse
list SparseCorpusStringEnumList {
    member: CorpusStringEnum
}

@sparse
list SparseCorpusIntEnumList {
    member: CorpusIntEnum
}

@sparse
list SparseSimpleStructList {
    member: SimpleStruct
}

map BooleanMap {
    key: String
    value: Boolean
}

map ByteMap {
    key: String
    value: Byte
}

map ShortMap {
    key: String
    value: Short
}

map IntegerMap {
    key: String
    value: Integer
}

map LongMap {
    key: String
    value: Long
}

map FloatMap {
    key: String
    value: Float
}

map DoubleMap {
    key: String
    value: Double
}

map BigIntegerMap {
    key: String
    value: BigInteger
}

map BigDecimalMap {
    key: String
    value: BigDecimal
}

map StringMap {
    key: String
    value: String
}

map BlobMap {
    key: String
    value: Blob
}

map TimestampMap {
    key: String
    value: NoFormatTimestamp
}

map CorpusStringEnumMap {
    key: String
    value: CorpusStringEnum
}

map CorpusIntEnumMap {
    key: String
    value: CorpusIntEnum
}

map SimpleStructMap {
    key: String
    value: SimpleStruct
}

map MapOfBooleanList {
    key: String
    value: BooleanList
}

map MapOfIntegerList {
    key: String
    value: IntegerList
}

map MapOfStringList {
    key: String
    value: StringList
}

map MapOfBlobList {
    key: String
    value: BlobList
}

map MapOfTimestampList {
    key: String
    value: TimestampList
}

map MapOfCorpusStringEnumList {
    key: String
    value: CorpusStringEnumList
}

map MapOfCorpusIntEnumList {
    key: String
    value: CorpusIntEnumList
}

map MapOfBooleanMap {
    key: String
    value: BooleanMap
}

map MapOfIntegerMap {
    key: String
    value: IntegerMap
}

map MapOfStringMap {
    key: String
    value: StringMap
}

map MapOfBlobMap {
    key: String
    value: BlobMap
}

map MapOfTimestampMap {
    key: String
    value: TimestampMap
}

map MapOfCorpusStringEnumMap {
    key: String
    value: CorpusStringEnumMap
}

map MapOfCorpusIntEnumMap {
    key: String
    value: CorpusIntEnumMap
}

map CorpusUnionMap {
    key: String
    value: CorpusUnion
}

@sparse
map SparseBooleanMap {
    key: String
    value: Boolean
}

@sparse
map SparseByteMap {
    key: String
    value: Byte
}

@sparse
map SparseShortMap {
    key: String
    value: Short
}

@sparse
map SparseIntegerMap {
    key: String
    value: Integer
}

@sparse
map SparseLongMap {
    key: String
    value: Long
}

@sparse
map SparseFloatMap {
    key: String
    value: Float
}

@sparse
map SparseDoubleMap {
    key: String
    value: Double
}

@sparse
map SparseBigIntegerMap {
    key: String
    value: BigInteger
}

@sparse
map SparseBigDecimalMap {
    key: String
    value: BigDecimal
}

@sparse
map SparseStringMap {
    key: String
    value: String
}

@sparse
map SparseBlobMap {
    key: String
    value: Blob
}

@sparse
map SparseTimestampMap {
    key: String
    value: NoFormatTimestamp
}

@sparse
map SparseCorpusStringEnumMap {
    key: String
    value: CorpusStringEnum
}

@sparse
map SparseCorpusIntEnumMap {
    key: String
    value: CorpusIntEnum
}

@sparse
map SparseSimpleStructMap {
    key: String
    value: SimpleStruct
}

enum CorpusStringEnum {
    FOO = "Foo"
    BAR = "Bar"
    BAZ = "Baz"
}

intEnum CorpusIntEnum {
    ONE = 1
    TWO = 2
    THREE = 3
}
