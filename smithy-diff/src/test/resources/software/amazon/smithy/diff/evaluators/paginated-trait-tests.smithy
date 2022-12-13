$version: "2.0"

namespace smithy.example

@paginated(inputToken: "token", outputToken: "token", items: "things", pageSize: "maxResults")
operation ListThings {
    input: ListThingsInput,
    output: ListThingsOutput,
}

structure ListThingsInput {
    token: String,
    otherToken: String,
    maxResults: Integer,
    otherMaxResults: Integer,
}

structure ListThingsOutput {
    token: String,
    otherToken: String,
    things: ThingList,
    otherThings: ThingList,
}

list ThingList {
    member: Thing,
}

structure Thing {}
