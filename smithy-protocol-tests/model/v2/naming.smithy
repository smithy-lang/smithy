$version: "2.0"

namespace smithy.protocoltests.corpus

// NamingProtocolTestService tests wire-name override traits.
//
// Every member here carries every naming trait, so a protocol either honors
// the one that belongs to it (restJson1 -> @jsonName, restXml -> @xmlName) or
// ignores all of them (awsJson, rpcv2Cbor, rpcv2Json). Each protocol writes
// cases for whichever behavior applies to it.
//
// The operations are a subset of the Core transition matrix, enough to reach a
// name in every position it can appear.
@mixin
service NamingProtocolTestService {
    operations: [
        NamedScalarMembers
        NamedStructOfScalars
        NamedListOfScalars
        NamedMapOfScalars
        NamedUnionMembers
    ]
}

operation NamedScalarMembers {
    input: NamedScalarStruct
    output: NamedScalarStruct
}

structure NamedScalarStruct {
    @jsonName("jsonBooleanMember")
    @xmlName("xmlBooleanMember")
    booleanMember: Boolean

    @jsonName("jsonByteMember")
    @xmlName("xmlByteMember")
    byteMember: Byte

    @jsonName("jsonShortMember")
    @xmlName("xmlShortMember")
    shortMember: Short

    @jsonName("jsonIntegerMember")
    @xmlName("xmlIntegerMember")
    integerMember: Integer

    @jsonName("jsonLongMember")
    @xmlName("xmlLongMember")
    longMember: Long

    @jsonName("jsonFloatMember")
    @xmlName("xmlFloatMember")
    floatMember: Float

    @jsonName("jsonDoubleMember")
    @xmlName("xmlDoubleMember")
    doubleMember: Double

    @jsonName("jsonBigIntegerMember")
    @xmlName("xmlBigIntegerMember")
    bigIntegerMember: BigInteger

    @jsonName("jsonBigDecimalMember")
    @xmlName("xmlBigDecimalMember")
    bigDecimalMember: BigDecimal

    @jsonName("jsonStringMember")
    @xmlName("xmlStringMember")
    stringMember: String

    @jsonName("jsonBlobMember")
    @xmlName("xmlBlobMember")
    blobMember: Blob

    @jsonName("jsonTimestampMember")
    @xmlName("xmlTimestampMember")
    timestampMember: NoFormatTimestamp

    @jsonName("jsonEnumMember")
    @xmlName("xmlEnumMember")
    enumMember: CorpusStringEnum

    @jsonName("jsonIntEnumMember")
    @xmlName("xmlIntEnumMember")
    intEnumMember: CorpusIntEnum
}

operation NamedStructOfScalars {
    input: NamedStructOfScalarsInput
    output: NamedStructOfScalarsOutput
}

structure NamedStructOfScalarsInput {
    @jsonName("jsonNested")
    @xmlName("xmlNested")
    nested: NamedSimpleStruct
}

structure NamedStructOfScalarsOutput {
    @jsonName("jsonNested")
    @xmlName("xmlNested")
    nested: NamedSimpleStruct
}

structure NamedSimpleStruct {
    @jsonName("jsonStringMember")
    @xmlName("xmlStringMember")
    stringMember: String

    @jsonName("jsonIntegerMember")
    @xmlName("xmlIntegerMember")
    integerMember: Integer

    @jsonName("jsonBooleanMember")
    @xmlName("xmlBooleanMember")
    booleanMember: Boolean

    @jsonName("jsonTimestampMember")
    @xmlName("xmlTimestampMember")
    timestampMember: NoFormatTimestamp
}

operation NamedListOfScalars {
    input: NamedListOfScalarsInput
    output: NamedListOfScalarsOutput
}

structure NamedListOfScalarsInput {
    @jsonName("jsonStrings")
    @xmlName("xmlStrings")
    strings: StringList

    @jsonName("jsonIntegers")
    @xmlName("xmlIntegers")
    integers: IntegerList

    @jsonName("jsonTimestamps")
    @xmlName("xmlTimestamps")
    timestamps: TimestampList
}

structure NamedListOfScalarsOutput {
    @jsonName("jsonStrings")
    @xmlName("xmlStrings")
    strings: StringList

    @jsonName("jsonIntegers")
    @xmlName("xmlIntegers")
    integers: IntegerList

    @jsonName("jsonTimestamps")
    @xmlName("xmlTimestamps")
    timestamps: TimestampList
}

operation NamedMapOfScalars {
    input: NamedMapOfScalarsInput
    output: NamedMapOfScalarsOutput
}

structure NamedMapOfScalarsInput {
    @jsonName("jsonStrings")
    @xmlName("xmlStrings")
    strings: StringMap

    @jsonName("jsonIntegers")
    @xmlName("xmlIntegers")
    integers: IntegerMap

    @jsonName("jsonTimestamps")
    @xmlName("xmlTimestamps")
    timestamps: TimestampMap
}

structure NamedMapOfScalarsOutput {
    @jsonName("jsonStrings")
    @xmlName("xmlStrings")
    strings: StringMap

    @jsonName("jsonIntegers")
    @xmlName("xmlIntegers")
    integers: IntegerMap

    @jsonName("jsonTimestamps")
    @xmlName("xmlTimestamps")
    timestamps: TimestampMap
}

operation NamedUnionMembers {
    input: NamedUnionMembersInput
    output: NamedUnionMembersOutput
}

structure NamedUnionMembersInput {
    @jsonName("jsonValue")
    @xmlName("xmlValue")
    value: NamedUnion
}

structure NamedUnionMembersOutput {
    @jsonName("jsonValue")
    @xmlName("xmlValue")
    value: NamedUnion
}

union NamedUnion {
    @jsonName("jsonStringMember")
    @xmlName("xmlStringMember")
    stringMember: String

    @jsonName("jsonIntegerMember")
    @xmlName("xmlIntegerMember")
    integerMember: Integer

    @jsonName("jsonBooleanMember")
    @xmlName("xmlBooleanMember")
    booleanMember: Boolean

    @jsonName("jsonListMember")
    @xmlName("xmlListMember")
    listMember: StringList

    @jsonName("jsonMapMember")
    @xmlName("xmlMapMember")
    mapMember: StringMap

    @jsonName("jsonStructMember")
    @xmlName("xmlStructMember")
    structMember: NamedSimpleStruct
}
