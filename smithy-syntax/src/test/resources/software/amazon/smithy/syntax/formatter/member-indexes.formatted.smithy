$version: "2.1"

namespace smithy.example

@mixin
structure Common {
    inherited: String
}

structure Indexed with [Common] {
     1. first: String

    10. second: String = "value" // trailing comment

    /// Third member
    @required
     2. third: {String: [Integer]}

     3. $inherited

    unindexed: String

    20. explicit: String
}

structure Other {
      1. one: String
    100. hundred: String
}
