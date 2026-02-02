$version: "2"

namespace smithy.example

use smithy.contracts#conditions

service LogFetcher {
    operations: [FetchLogs]
}

operation FetchLogs {
    input: FetchLogsInput
}

@conditions({
    StartBeforeEnd: {
        documentation: "The start time must be strictly less than the end time",
        expression: "start < end"
    }
})
structure FetchLogsInput {
    @required
    start: Timestamp

    @required
    end: Timestamp
}

@conditions({
    NoKeywords: {
        documentation: "The name cannot contain either 'id' or 'name', as these are reserved keywords"
        expression: "!contains(@, 'id') && !contains(@, 'name')"
    }
})
string Name
