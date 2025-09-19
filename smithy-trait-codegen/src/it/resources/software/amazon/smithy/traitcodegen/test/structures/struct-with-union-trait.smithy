$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.structures#StructWithUnionTrait

@StructWithUnionTrait(
    myUnion: { stringVariant: "123" }
)
structure myStruct {}
