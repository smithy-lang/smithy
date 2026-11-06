// A version that omits the minor version resolves to the latest known minor
// version, so "2" grants the 2.1-only member index shorthand.
$version: "2"

namespace smithy.protocols

// Define idx locally so this model is self-contained.
@trait(selector: "member")
integer idx

@mixin
structure Common {
    inherited: String
}

structure Indexed with [Common] {
    @required
    1. first: String

    12. second: String = "value"

    @idx(20)
    20. equal: String

    3. $inherited
}
