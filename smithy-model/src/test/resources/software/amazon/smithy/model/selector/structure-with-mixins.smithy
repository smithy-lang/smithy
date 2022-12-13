$version: "2.0"

namespace smithy.example

@mixin
structure Mixin1 {}

@mixin
structure Mixin2 with [Mixin1] {}

structure Concrete with [Mixin2] {}

structure NoMixins {}

@mixin
structure UnusedMixin {}
