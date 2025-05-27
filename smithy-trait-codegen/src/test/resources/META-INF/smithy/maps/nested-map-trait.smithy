$version: "2.0"

namespace test.smithy.traitcodegen.maps

@trait
map NestedMapTrait {
    key: String
    value:ItemsMap
}

map ItemsMap {
    key: String
    value: ItemsMapEntry
}

map ItemsMapEntry {
    key: String
    value: String
}