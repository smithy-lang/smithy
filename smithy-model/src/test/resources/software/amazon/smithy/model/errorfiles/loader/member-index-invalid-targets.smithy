$version: "2.1"

namespace smithy.protocols

// Define idx locally so this model is self-contained.
@trait(selector: ":is(structure, union) :not([trait|mixin]) > member")
integer idx

list InvalidList {
    1. member: String
}

map InvalidMap {
    1. key: String
    2. value: String
}
