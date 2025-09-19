$version: "2.0"

namespace test.smithy.traitcodegen.maps

@trait
map StringToUnionMap{
    key: String,
    value: MyUnion
}

union MyUnion {
    unitVariant: Unit

    stringVariant: String

    integerVariant: Integer

    timestampVariant: Timestamp
}
