$version: "2"

namespace test.smithy.traitcodegen.unions

@trait
@documentation("This is my union trait!!!")
union UnionTrait {
    @documentation("This is my union trait's unit variant!!!")
    unitVariant: Unit

    @idRef
    stringVariant: String

    @documentation("This is my union trait's integer variant!!!")
    integerVariant: Integer

    structVariant: NestedStructA

    unionVariant: NestedUnionA

    listVariant: StringListA

    setVariant: StringSetA

    mapVariant: StringStringMapA

    @timestampFormat("http-date")
    timestampVariant: Timestamp
}

structure NestedStructA {
    memberA: String
    memberB: Float
}

union NestedUnionA {
    unitVariant: Unit

    stringVariant: String

    integerVariant: Integer

    structVariant: NestedStructA

    unionVariant: NestedUnionB

    listVariant: StringListA

    setVariant: StringSetA

    mapVariant: StringStringMapA

    @timestampFormat("epoch-seconds")
    timestampVariant: Timestamp
}

union NestedUnionB {
    c: String
}

list StringListA {
    member: String
}

@uniqueItems
list StringSetA {
    member: String
}

map StringStringMapA {
    key: String
    value: String
}
