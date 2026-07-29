$version: "2.1"

namespace smithy.protocols

// Define idx locally so this model is self-contained.
@trait(selector: ":is(structure, union) :not([trait|mixin]) > member")
integer idx

structure Indexed {
    @idx(2)
    1. first: String
}
