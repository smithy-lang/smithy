$version: "2.0"

namespace smithy.example

@mixin
structure Foo {
    foo: String
}

structure Baz {
    invalid: Foo // this is not allowed because Foo is a mixin
}
