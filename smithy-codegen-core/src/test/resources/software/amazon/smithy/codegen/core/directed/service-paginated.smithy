$version: "1.0"

namespace smithy.example

@paginated(inputToken: "nextToken", outputToken: "nextToken", items: "items", pageSize: "maxResults")
service Foo {
    operations: [ListA, ListB]
}

@readonly
@paginated
operation ListA {
    input: ListAInput,
    output: ListAOutput
}

@input
structure ListAInput {
    maxResults: Integer,
    nextToken: String
}

@output
structure ListAOutput {
    nextToken: String,
    items: StringList
}

list StringList {
    member: String
}

@readonly
@paginated
operation ListB {
    input: ListBInput,
    output: ListBOutput
}

@input
structure ListBInput {
    maxResults: Integer,
    nextToken: String
}

@output
structure ListBOutput {
    nextToken: String,
    items: StringList
}
