$version: "2.0"

namespace smithy.example

@mixin
structure C {
    c: String
}

structure D with [C] {
    d: String
}
