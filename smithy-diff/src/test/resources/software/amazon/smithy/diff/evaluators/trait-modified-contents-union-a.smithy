$version: "2.0"

namespace smithy.example

@aTrait(baz: {foo: "a"})
string Foo

@aTrait(baz: {baz: "a"})
string Boo

@aTrait(baz: {bar: "a"})
string Moo

@trait
@tags(["diff.contents"])
structure aTrait {
    baz: NestedUnion
}

union NestedUnion {
    @tags(["diff.error.update"])
    foo: String,

    @tags(["diff.error.update"])
    baz: String,

    // changing this value, if set, is fine.
    bar: String,
}
