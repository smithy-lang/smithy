$version: "2.0"

namespace smithy.example

@aTrait(foo: {baz: "a", bam: "b", boo: "c"})
string Foo

@aTrait
string Baz

@aTrait(foo: {baz: "1", bam: "2", boo: "3"})
string Bar

@aTrait(foo: {baz: "1", bam: "2", boo: "3"})
string Bam

@aTrait(foo: {baz: "1", bam: "2", boo: "3"})
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
