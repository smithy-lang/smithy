$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.maps#StringDocumentMap

@StringDocumentMap(
    a: { a : "a" }
    b: {
        b : "b"
        c : "c"
    }
)
structure myStruct {
}
