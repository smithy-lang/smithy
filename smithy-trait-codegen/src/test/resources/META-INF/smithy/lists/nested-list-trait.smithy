$version: "2.0"

namespace test.smithy.traitcodegen.lists

@trait
list NestedListTrait {
    member:ItemsList
}

list ItemsList {
    member: ItemsListEntry
}

list ItemsListEntry {
    member: String
}