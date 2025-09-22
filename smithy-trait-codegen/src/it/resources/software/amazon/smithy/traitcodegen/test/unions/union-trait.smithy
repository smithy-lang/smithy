$version: "2"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.unions#UnionTrait

@UnionTrait(
    listVariant: ["1"]
)
structure myStruct {}
