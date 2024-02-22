$version: "2"

namespace com.example

@mixin
structure MixinA {
    @pattern("foo")
    @required
    member: String
}

@mixin
structure MixinB {
    @pattern("bar")
    @internal
    member: String
}

structure FinalStructure with [MixinA, MixinB] {
    @pattern("baz")
    member: String
}
