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
operation StreamingInput {
    input: StreamingInputOutput
}

structure StreamingInputOutput {
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
operation StreamingOutput {
    input: StreamingInputOutput
}
