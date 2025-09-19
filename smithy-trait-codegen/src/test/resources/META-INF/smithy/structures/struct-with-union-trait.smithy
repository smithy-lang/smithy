$version: "2.0"

namespace test.smithy.traitcodegen.structures

@trait
structure StructWithUnionTrait {
    myUnion: MyUnion
}

union MyUnion {
    unitVariant: Unit

    stringVariant: String

    integerVariant: Integer

    timestampVariant: Timestamp
}
