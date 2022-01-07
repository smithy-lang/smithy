$version: "2.0"

namespace smithy.example

@mixin
structure Foo {
    foo: String
    bar: String
}

structure Baz with [Foo] {
    foo: Integer // cannot redefine mixin members with a different target!
    bar: String // This is allowed because the target hasn't changed
}

@mixin
structure Bam with [Foo] {
    foo: Integer // cannot redefine mixin members with a different target!
    bar: String // This is allowed because the target hasn't changed
}

structure Boo with [Bam] {
    foo: Long // cannot redefine mixin members with a different target!
    bar: String // This is allowed because the target hasn't changed
}
