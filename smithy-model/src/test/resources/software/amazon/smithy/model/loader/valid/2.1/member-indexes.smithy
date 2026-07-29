$version: "2.1"

namespace smithy.protocols

// Define idx locally so this generated model is self-contained.
@trait(selector: "member")
integer idx

@mixin
structure Common {
    inherited: String
}

structure Indexed with [Common] {
    @required
    1. first: String

    12 . second : String = "value",

    2147483644. index2147483644: String
    2147483645. index2147483645: String
    2147483646. index2147483646: String

    @idx(20)
    20. equal: String

    3. $inherited

    2147483647. values: [String] // trailing comment
}
