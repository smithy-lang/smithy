$version: "2.0"

namespace test.smithy.traitcodegen.lists


@trait
@uniqueItems
list NestedUniqueItemsListTrait {
    member:UniqueItemsList
}

@uniqueItems
list UniqueItemsList {
    member: UniqueItemsListEntry
}

@uniqueItems
list UniqueItemsListEntry {
    member: String
}
