$version: "2.0"

namespace smithy.example

@paginated(inputToken: "nextToken", outputToken: "nextToken", items: "items", pageSize: "maxResults")
service Foo {
    resources: [
        TheFoo
    ]
}

resource TheFoo {
    identifiers: { id: String }
    list: ListFoo
}

@readonly
@paginated
operation ListFoo {
    input := {
        maxResults: Integer
        nextToken: String
    }
    output := with [Paginated] {
        status: Status
        items: StringList
        instruction: Instruction
        facecard: FaceCard
    }
}

list StringList {
    member: String
}

@enum([
    {
        name: "GOOD"
        value: "GOOD"
    }
    {
        name: "BAD"
        value: "BAD"
    }
])
string Status

intEnum FaceCard {
    JACK = 1
    QUEEN = 2
    KING = 3
}

union Instruction {
    continueIteration: Unit
    stopIteration: Unit
}

@mixin
structure Paginated {
    nextToken: String
}
