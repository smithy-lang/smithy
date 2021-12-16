$version: "2.0"

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
operation StreamingOutput {
    output: StreamingOutputOutput
}

@output
structure StreamingOutputOutput {
    messages: Messages,
}
