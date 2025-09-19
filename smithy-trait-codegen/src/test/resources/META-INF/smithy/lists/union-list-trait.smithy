$version: "2.0"

namespace test.smithy.traitcodegen.lists

@trait
list UnionListTrait {
    member: MyUnion
}

union MyUnion {
    unitVariant: Unit

    stringVariant: String

    integerVariant: Integer

    timestampVariant: Timestamp
}
