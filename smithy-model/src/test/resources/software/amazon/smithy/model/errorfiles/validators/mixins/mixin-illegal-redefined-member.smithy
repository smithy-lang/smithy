$version: "1.1"

namespace smithy.example

@mixin
structure Foo {
    foo: String
}

structure Baz with Foo {
    foo: String // cannot redefine mixin members!
}

@mixin
structure Bam with Foo {
    foo: String // cannot redefine mixin members!
}

structure Boo with Bam {
    foo: String // cannot redefine mixin members!
}
