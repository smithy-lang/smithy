$version: "2"

namespace smithy.example

use smithy.contracts#conditions

service LogFetcher {
    operations: [FetchLogs]
}

operation FetchLogs {
    input: FetchLogsInput
}

@conditions([
    {
        id: "StartBeforeEnd",
        description: "The start time must be strictly less than the end time",
        expression: "start < end"
    }
])
structure FetchLogsInput {
    @required
    start: Timestamp

    @required
    end: Timestamp
}

@conditions([
    {
        id: "NoKeywords",
        expression: "!contains(@, 'id') && !contains(@, 'name')"
    }
])
string Foo