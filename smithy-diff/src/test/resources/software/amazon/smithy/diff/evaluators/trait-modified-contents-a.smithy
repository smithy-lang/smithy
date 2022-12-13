$version: "2.0"

namespace smithy.example

@aTrait(foo: "hi", baz: {foo: "bye"})
string Foo

@trait
@tags(["diff.contents"])
structure aTrait {
    @tags(["diff.error.remove"])
    foo: String,

    @tags(["diff.warning.const"])
    bar: String,

    baz: Nested
}

structure Nested {
    @tags(["diff.error.const"])
    foo: String,
}
