$version: "1.0"

namespace smithy.example

integer MyInteger

structure Foo {
    bar: Integer,
    baz: MyInteger,

    @box
    bam: PrimitiveInteger,

    // An invalid trait doesn't break the loading process or prevent upgrades.
    @thisTraitDoesNotExist
    boo: String,

    // An invalid target doesn't break the loading process or prevent upgrades.
    bux: InvalidTarget
}
