$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.lists#UnionListTrait

@UnionListTrait([
    {
        stringVariant: "123"
    }
    {
        integerVariant: 123
    }
])
structure myStruct {}
