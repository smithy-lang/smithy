$version: "2.1"

namespace smithy.protocols

@trait(
    selector: ":is(structure, union) :not([trait|mixin]) > member"
)
integer idx

@mixin
structure Common {
    inherited: String
}

structure Indexed {
    @required
    1. value: String
}

structure Mixed with [Common] {}

apply Mixed$inherited @idx(7)
