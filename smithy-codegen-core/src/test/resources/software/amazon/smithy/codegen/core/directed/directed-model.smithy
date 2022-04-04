$version: "1.0"

namespace smithy.example

@paginated(inputToken: "nextToken", outputToken: "nextToken", items: "items", pageSize: "maxResults")
service Foo {
    operations: [ListFoo]
}

@readonly
@paginated
operation ListFoo {
    input: ListFooInput,
    output: ListFooOutput
}

@input
structure ListFooInput {
    maxResults: Integer,
    nextToken: String
}

@output
structure ListFooOutput {
    nextToken: String,
    items: StringList
}

list StringList {
    member: String
}
