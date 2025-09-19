$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.maps#StringToUnionMap

@StringToUnionMap(
    one: { stringVariant: "123" }
    two: { integerVariant: 123 }
)
structure myStruct {}
