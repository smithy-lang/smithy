$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.lists#DocumentListTrait

@DocumentListTrait([
    {
        a: "a"
    }
    {
        b: "b"
        c: 1
    }
    "string"
])
structure myStruct {}
