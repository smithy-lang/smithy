$version: "2.0"

namespace smithy.example

@deprecated
@documentation("B doc")
@internal
@since("B")
@tags(["b"])
structure Both {}

@deprecated
@documentation("A doc")
@internal
@since("A")
@tags(["a"])
structure Reversed {}

@deprecated
@documentation("B doc")
@internal
@since("local")
@tags(["b"])
structure LocalWins {}

@deprecated
@documentation("B doc")
@internal
@since("C")
@tags(["b"])
structure Nested {}

@deprecated
@documentation("B doc")
@internal
@since("B")
@tags(["b"])
structure NestedWithoutOverride {}

@deprecated
@documentation("B doc")
@internal
@since("B")
@tags(["b"])
structure SimpleThenComposed {}

@deprecated
@documentation("A doc")
@internal
@since("A")
@tags(["a"])
structure ComposedThenSimple {}
