$version: "2.0"

namespace test.smithy.traitcodegen.maps

@trait
map StringToStructMap {
    key: String
    value: MapValue
}

@private
structure MapValue {
    a: String
    b: Integer
}
