$version: "2.0"

namespace test.smithy.traitcodegen.lists

@trait
list NestedBooleanListTrait {
    member: BooleanItemsList
}

list BooleanItemsList {
    member: BooleanItemsListEntry
}

list BooleanItemsListEntry {
    member: Boolean
}
