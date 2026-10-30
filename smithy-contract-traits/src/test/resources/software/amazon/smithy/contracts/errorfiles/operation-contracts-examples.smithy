$version: "2"

namespace smithy.example

use smithy.contracts#contracts

@contracts({
    StartBeforeEnd: {
        documentation: "The requested start time must be strictly less than the end time"
        expression: "input.start < input.end"
    }
})
@examples([
    {
        title: "Valid range"
        input: {start: 1, end: 2}
    }
    {
        title: "Invalid range"
        input: {start: 5, end: 3}
    }
])
operation FetchLogs {
    input: FetchLogsInput
}

@input
structure FetchLogsInput {
    @required
    start: Long

    @required
    end: Long
}
