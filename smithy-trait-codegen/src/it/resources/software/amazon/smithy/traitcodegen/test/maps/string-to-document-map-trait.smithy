$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.maps#StringDocumentMap

@StringDocumentMap(
    a: { a: "a" }
    b: { b: "b", c: 1 }
    c: "stuff"
    d: 1
)
structure myStruct {}
