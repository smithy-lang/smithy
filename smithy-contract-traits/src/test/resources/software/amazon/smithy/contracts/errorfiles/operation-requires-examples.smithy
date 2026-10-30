$version: "2"

namespace smithy.example

use smithy.contracts#contracts

@contracts({
    StartBeforeEndOnSuccess: {
        documentation: "On a successful call, the start time must be strictly less than the end time"
        expression: "requires(@, input.start < input.end)"
    }
})
@examples([
    {
        title: "Success within contract"
        input: {start: 1, end: 2}
        output: {}
    }
    {
        title: "Success violating contract"
        input: {start: 5, end: 3}
        output: {}
    }
    {
        title: "Failure is exempt from the precondition"
        input: {start: 5, end: 3}
        error: {shapeId: "smithy.example#BadRange", content: {}}
    }
])
operation FetchLogs {
    input: FetchLogsInput
    output: FetchLogsOutput
    errors: [BadRange]
}

@input
structure FetchLogsInput {
    @required
    start: Long

    @required
    end: Long
}

@output
structure FetchLogsOutput {}

@error("client")
structure BadRange {}
