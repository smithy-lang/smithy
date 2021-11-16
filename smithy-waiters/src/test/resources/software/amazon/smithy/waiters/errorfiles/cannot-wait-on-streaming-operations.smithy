namespace smithy.example

use smithy.waiters#waitable

@waitable(
    Success: {
        documentation: "A",
        acceptors: [
            {
                state: "success",
                matcher: {success: true}
            }
        ]
    }
)
@suppress(["OperationMissingOutput"])
operation StreamingInput {
    input: StreamingInputInput
}

@input
structure StreamingInputInput {
    messages: Messages,
}

@streaming
union Messages {
    success: SuccessMessage
}

structure SuccessMessage {}

@waitable(
    Success: {
        documentation: "B",
        acceptors: [
            {
                state: "success",
                matcher: {success: true}
            }
        ]
    }
)
@suppress(["OperationMissingInput"])
operation StreamingOutput {
    output: StreamingOutputOutput
}

@output
structure StreamingOutputOutput {
    messages: Messages,
}
