$version: "2.0"

namespace test.smithy.traitcodegen.documents

@trait
structure structWithNestedDocument {
    doc: nestedDoc
}

@private
document nestedDoc
