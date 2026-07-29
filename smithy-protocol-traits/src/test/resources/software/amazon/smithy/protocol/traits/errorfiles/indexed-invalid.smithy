$version: "2.0"

namespace smithy.example

use smithy.protocols#idx
use smithy.protocols#indexed

structure Duplicate {
    @idx(1)
    first: String

    @idx(1)
    second: String
}

structure Gaps {
    @idx(1)
    first: String

    @idx(3)
    third: String

    @idx(5)
    fifth: String
}

structure NonOneStart {
    @idx(2)
    second: String
}

structure Partial {
    @idx(1)
    first: String

    second: String
}

@mixin
structure Common {
    inherited: String
}

structure UsesMixin with [Common] {
    @idx(2)
    local: String

    $inherited
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
    input := {
        missing: String
    }
    output := {
        missing: String
    }
}

structure OutsideClosure {
    unaffected: String
}
