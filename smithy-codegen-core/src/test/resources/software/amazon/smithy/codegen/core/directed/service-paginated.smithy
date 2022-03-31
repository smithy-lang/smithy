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

structure ListAInput {
    maxResults: Integer,
    nextToken: String
}

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

structure ListBInput {
    maxResults: Integer,
    nextToken: String
}

structure ListBOutput {
    nextToken: String,
    items: StringList
}
