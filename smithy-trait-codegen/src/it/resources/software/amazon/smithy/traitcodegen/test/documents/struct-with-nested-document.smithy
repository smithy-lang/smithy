$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.documents#structWithNestedDocument

@structWithNestedDocument(
    doc: { foo: "bar", fizz: "buzz" }
)
structure myStruct {}
