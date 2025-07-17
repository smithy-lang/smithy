$version: "2.0"

namespace test.smithy.traitcodegen.maps

@trait
map NestedStringUniqueItemMapTrait {
    key: String
    value: UniqueItemsList
}

@uniqueItems
list UniqueItemsList{
    member: UniqueItemsMapEntry
}

map UniqueItemsMapEntry {
    key: String
    value: String
}
