$version: "1.1"

namespace smithy.example

@mixin
structure C {
    c: String
}

structure D with C {
    d: String
}
