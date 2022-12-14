$version: "2.0"

namespace smithy.example

enum Foo {
    @tags(["filter"])
    FILTER

    @tags(["keep"])
    KEEP
}

intEnum Bar {
    @tags(["filter"])
    @enumValue(1)
    FILTER

    @tags(["keep"])
    @enumValue(2)
    KEEP
}

@trait
@tags(["filter"])
string MyTrait

@mixin
structure MyMixin {
    @required
    baz: String
}

structure StructForMixin with [MyMixin] {
    bar: String
}

apply StructForMixin$baz @MyTrait("hello")
