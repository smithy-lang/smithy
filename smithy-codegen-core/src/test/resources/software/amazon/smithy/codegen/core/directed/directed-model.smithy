$version: "1.0"

namespace smithy.example

@paginated(inputToken: "nextToken", outputToken: "nextToken", items: "items", pageSize: "maxResults")
service Foo {
    resources: [TheFoo]
}

resource TheFoo {
    identifiers: {id: String},
    list: ListFoo
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
    status: Status,
    items: StringList,
    instruction: Instruction,
}

list StringList {
    member: String
}

@enum([
    {name: "GOOD", value: "GOOD"},
    {name: "BAD", value: "BAD"}
])
string Status

union Instruction {
    continueIteration: Unit,
    stopIteration: Unit
}
