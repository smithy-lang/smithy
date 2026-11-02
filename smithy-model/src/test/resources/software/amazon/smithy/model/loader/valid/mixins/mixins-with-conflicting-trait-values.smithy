$version: "2.0"

namespace smithy.example

@documentation("A doc")
@internal
@since("A")
@tags(["a"])
@mixin
structure A {}

@deprecated
@documentation("B doc")
@since("B")
@tags(["b"])
@mixin
structure B {}

// Traits applied to mixins that come later in the list of mixins take
// precedence over traits applied to mixins that come earlier in the list.
structure Both with [A, B] {}

structure Reversed with [B, A] {}

// Traits applied directly to a shape take precedence over inherited traits.
@since("local")
structure LocalWins with [A, B] {}

// A mixin that uses other mixins passes along its resolved traits.
@since("C")
@mixin
structure C with [A, B] {}

structure Nested with [C] {}

@mixin
structure D with [A, B] {}

structure NestedWithoutOverride with [D] {}

// A mixin composed of other mixins takes part in the ordering using its
// resolved traits.
structure SimpleThenComposed with [A, D] {}

structure ComposedThenSimple with [D, A] {}
