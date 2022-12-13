$version: "2.0"

namespace smithy.example

@aTrait(baz: {foo: "b"})
string Foo

@aTrait(baz: {baz: "b"})
string Boo

@aTrait(baz: {bar: "b"})
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
