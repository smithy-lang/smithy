$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.structures#StructWithUniqueItemsListTrait

@StructWithUniqueItemsListTrait(
    name: "a"
    items: [
        ["b", "c"]
        ["d", "e"]
    ]
)
structure myStruct {}
