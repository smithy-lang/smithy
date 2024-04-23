$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.structures#structureTrait

@structureTrait(
    fieldA: "first"
    fieldB: false
    fieldC: { fieldN: "nested", fieldQ: true, fieldZ: "A" }
    fieldD: ["a", "b", "c"]
    fieldE: { a: "one", b: "two" }
    fieldF: 100.01
    fieldG: 100
)
structure myStruct {}
