$version: "2.0"

namespace smithy.example

use smithy.protocols#idx
use smithy.protocols#indexed

@mixin
structure Common {
    inherited: String
}

structure OutsideClosure {
    unindexed: String
}

@indexed
service FirstService {
    version: "2026-07-24"
    operations: [Get]
}

@indexed
service SecondService {
    version: "2026-07-24"
    operations: [Get]
}

operation Get {
    input := with [Common] {
        @idx(3)
        nested: Nested

        @idx(1)
        $inherited

        @idx(2)
        choice: Choice
    }
    output := {
        @idx(1)
        empty: Empty
    }
}

structure Nested {
    @idx(2)
    second: String

    @idx(1)
    first: String
}

union Choice {
    @idx(2)
    right: String

    @idx(1)
    left: String
}

structure Empty {}
