$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.idref#IdRefNestedKeyMap

@IdRefNestedKeyMap(
    outer: {
        "test.smithy.traitcodegen#IdRefTarget1": "a"
        "test.smithy.traitcodegen#IdRefTarget2": "b"
    }
)
structure myStruct {}

string IdRefTarget1

string IdRefTarget2
