$version: "2.0"

namespace test.smithy.traitcodegen

@structWithNestedDocument(
    doc: {
        foo: "bar"
        fizz: "buzz"
    }
)
structure myStruct {}
