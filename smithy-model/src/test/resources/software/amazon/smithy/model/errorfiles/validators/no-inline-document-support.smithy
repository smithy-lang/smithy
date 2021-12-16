$version: "2.0"

namespace smithy.example

@trait
@protocolDefinition(noInlineDocumentSupport: true)
structure noDocuments {}

@trait
@protocolDefinition
structure yesDocuments {}

@noDocuments
@yesDocuments
service FooService {
    version: "2012-06-11",
    operations: [Foo]
}

operation Foo {
    input: FooInput,
    output: FooOutput
}

@input
structure FooInput {
    doc: InlineDocument,
}

@output
structure FooOutput {}

document InlineDocument

@yesDocuments
service ValidService {
    version: "2012-06-11",
    operations: [Foo]
}
