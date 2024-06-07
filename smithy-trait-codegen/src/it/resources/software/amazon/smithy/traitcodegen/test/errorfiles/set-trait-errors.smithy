$version: "2.0"

namespace test.smithy.traitcodegen.uniqueitems

// Doesnt have unique items. Expect failure
@NumberSetTrait([1, 1, 3, 4])
structure repeatedNumberValues {}

@StringSetTrait(["a", "a", "b"])
structure repeatedStringValues {}
