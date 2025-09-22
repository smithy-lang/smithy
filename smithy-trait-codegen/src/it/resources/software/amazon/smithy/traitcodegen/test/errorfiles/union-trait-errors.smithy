$version: "2.0"

namespace test.smithy.traitcodegen.unions

use test.smithy.traitcodegen.unions#UnionTrait

@UnionTrait(
    stringVariant: "1"
    listVariant: ["1"]
)
structure myStructA {}

@UnionTrait({})
structure myStructB {}

@UnionTrait(
    fooVariant:"!"
)
structure myStructC {}
