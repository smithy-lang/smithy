$version: "2.0"

namespace smithy.example

@aTrait(foo: {baz: "a", bam: "B", boo: "c"}) // changed B
string Foo

@aTrait(foo: {baz: "a", bam: "B", boo: "c"}) // Added "foo"
string Baz

@aTrait(foo: {baz: "1", bam: "2"}) // removed boo
string Bar

@aTrait(foo: {baz: "1", bam: "2", boo: "3", qux: "4"}) // added qux
string Bam

@aTrait // removed foo
string Qux

@trait
@tags(["diff.contents"])
structure aTrait {
    // Can't remove this value, but you can add it or change it.
    @tags(["diff.error.remove"])
    foo: ATraitMap,
}

map ATraitMap {
    key: String,

    // Cannot remove or update a list value at a given index.
    @tags(["diff.warning.const"])
    value: String,
}
