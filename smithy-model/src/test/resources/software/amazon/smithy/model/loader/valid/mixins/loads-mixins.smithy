$version: "2.0"

namespace smithy.example

@mixin
structure A {}

structure B with A {}

@mixin
structure C {}

@mixin
structure D with C {}

@mixin
structure E with D {}

structure F with A, E {}
