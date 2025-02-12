$version: "2.0"

namespace smithy.example

@mixin
structure FooMixin {
    bar: String
}

structure Foo with [FooMixin] {
    foo: String
}
