$version: "1.1"

namespace smithy.example

@mixin
structure Mixin {
    foo: String
}

structure UsesMixin with Mixin {
    baz: String
}
