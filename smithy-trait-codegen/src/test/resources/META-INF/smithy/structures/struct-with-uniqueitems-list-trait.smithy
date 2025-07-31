$version: "2.0"

namespace test.smithy.traitcodegen.structures

@trait
structure StructWithUniqueItemsListTrait {
    name: String
    items: UniqueItemsList
}

@uniqueItems
list UniqueItemsList {
    member: SecondUniqueItemsList
}

@uniqueItems
list SecondUniqueItemsList {
    member: String
}
