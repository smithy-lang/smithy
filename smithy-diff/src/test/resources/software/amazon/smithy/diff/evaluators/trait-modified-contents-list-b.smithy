$version: "2.0"

namespace smithy.example

@aTrait(foo: ["a", "B", "c"]) // Changed "b" -> "B"
string Foo

@aTrait(foo: ["a", "b", "c"]) // added "foo"
string Baz

@aTrait // Removed "foo"
string Bar

@aTrait(foo: ["1", "2"]) // removed "3"
string Bam

@aTrait(foo: ["1", "2", "3", "4"]) // added "4". This is ignored.
string Qux

@trait
@tags(["diff.contents"])
structure aTrait {
    // Can't remove this value, but you can add it or change it.
    @tags(["diff.error.remove"])
    foo: ATraitList,
}

list ATraitList {
    // Cannot remove or update a list value at a given index.
    @tags(["diff.danger.const"])
    member: String,
}
