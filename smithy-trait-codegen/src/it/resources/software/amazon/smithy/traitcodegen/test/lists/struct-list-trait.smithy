$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.lists#StructureListTrait

@StructureListTrait([
    {
        a: "first"
        b: 1
        c: "other"
    }
    {
        a: "second"
        b: 2
        c: "more"
    }
])
structure myStruct {}
