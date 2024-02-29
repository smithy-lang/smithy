$version: "2.0"

namespace test.smithy.traitcodegen.lists

@StringListTrait([1, 2, 3])
structure badInputTypes {}

@StringListTrait(1)
structure badInputType {}

@StringListTrait(["a", 2, "b"])
structure inconsistentInputTypes {}
