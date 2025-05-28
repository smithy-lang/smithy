$version: "2.0"

namespace test.smithy.traitcodegen.structures

@trait
structure StructWithListOfMapTrait {
    name: String
    items: ItemsList
}

list ItemsList {
    member: ItemsListEntry
}

map ItemsListEntry {
    key: String,
    value: String
}
