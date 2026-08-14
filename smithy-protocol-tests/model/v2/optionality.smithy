$version: "2.0"

namespace smithy.protocoltests.corpus

// OptionalityProtocolTestService tests @required, @default, and
// @clientOptional semantics.
@mixin
service OptionalityProtocolTestService {
    operations: [
        DefaultScalars
        DefaultCollections
        NestedDefaults
        RequiredMembers
        NullSparseMembers
        ClientOptionalDefaults
    ]
}

operation DefaultScalars {
    input: DefaultScalarsInput
    output: DefaultScalarsOutput
}

structure DefaultScalarsInput with [DefaultScalarsMixin] {}

structure DefaultScalarsOutput with [DefaultScalarsMixin] {}

@mixin
structure DefaultScalarsMixin {
    defaultBoolean: Boolean = false
    defaultByte: Byte = 0
    defaultShort: Short = 0
    defaultInteger: Integer = 0
    defaultLong: Long = 0
    defaultFloat: Float = 0
    defaultDouble: Double = 0
    defaultString: String = ""
    defaultBlob: Blob = ""
    defaultEnum: CorpusStringEnum = "Foo"
    defaultIntEnum: CorpusIntEnum = 1
    zeroBoolean: Boolean = false
    zeroByte: Byte = 0
    zeroShort: Short = 0
    zeroInteger: Integer = 0
    zeroLong: Long = 0
    zeroFloat: Float = 0
    zeroDouble: Double = 0
    emptyString: String = ""
    emptyBlob: Blob = ""
}

operation DefaultCollections {
    input: DefaultCollectionsInput
    output: DefaultCollectionsOutput
}

structure DefaultCollectionsInput with [DefaultCollectionsMixin] {}

structure DefaultCollectionsOutput with [DefaultCollectionsMixin] {}

@mixin
structure DefaultCollectionsMixin {
    defaultList: StringList = []
    defaultMap: StringMap = {}
}

operation NestedDefaults {
    input: NestedDefaultsInput
    output: NestedDefaultsOutput
}

structure NestedDefaultsInput {
    topLevel: TopLevelWithDefaults
}

structure NestedDefaultsOutput {
    topLevel: TopLevelWithDefaults
}

structure TopLevelWithDefaults {
    @required
    nested: NestedWithDefaults

    nestedList: NestedWithDefaultsList

    nestedMap: NestedWithDefaultsMap
}

structure NestedWithDefaults {
    greeting: String = "hello"
    count: Integer = 0
    inner: InnerWithDefaults
}

structure InnerWithDefaults {
    farewell: String = "goodbye"
}

list NestedWithDefaultsList {
    member: NestedWithDefaults
}

map NestedWithDefaultsMap {
    key: String
    value: NestedWithDefaults
}

operation RequiredMembers {
    input: RequiredMembersInput
    output: RequiredMembersOutput
}

structure RequiredMembersInput with [RequiredMembersMixin] {}

structure RequiredMembersOutput with [RequiredMembersMixin] {}

@mixin
structure RequiredMembersMixin {
    @required
    requiredString: String

    @required
    requiredInteger: Integer

    @required
    requiredBoolean: Boolean

    @required
    requiredList: StringList

    @required
    requiredMap: StringMap

    @required
    requiredStringWithDefault: String = "default"

    @required
    requiredIntegerWithDefault: Integer = 0

    @required
    requiredBooleanWithDefault: Boolean = false

    @required
    requiredListWithDefault: StringList = []

    @required
    requiredMapWithDefault: StringMap = {}
}

operation NullSparseMembers {
    input: NullSparseMembersInput
    output: NullSparseMembersOutput
}

structure NullSparseMembersInput {
    sparseStringList: SparseStringList
    sparseStringMap: SparseStringMap
    sparseStructList: SparseSimpleStructList
    sparseStructMap: SparseSimpleStructMap
}

structure NullSparseMembersOutput {
    sparseStringList: SparseStringList
    sparseStringMap: SparseStringMap
    sparseStructList: SparseSimpleStructList
    sparseStructMap: SparseSimpleStructMap
}

operation ClientOptionalDefaults {
    input: ClientOptionalDefaultsInput
    output: ClientOptionalDefaultsOutput
}

structure ClientOptionalDefaultsInput with [ClientOptionalMixin] {}

structure ClientOptionalDefaultsOutput with [ClientOptionalMixin] {}

@mixin
structure ClientOptionalMixin {
    @clientOptional
    optionalInteger: Integer = 0

    @clientOptional
    optionalString: String = ""

    @clientOptional
    optionalBoolean: Boolean = false
}
