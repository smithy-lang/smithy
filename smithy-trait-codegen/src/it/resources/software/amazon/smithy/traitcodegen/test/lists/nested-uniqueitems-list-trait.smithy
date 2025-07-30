$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.lists#NestedUniqueItemsListTrait

@NestedUniqueItemsListTrait([
    [
        ["a", "ab", "c", "bc"]
        ["b", "ba", "ab", "aa"]
    ]
])
structure myStruct {}
