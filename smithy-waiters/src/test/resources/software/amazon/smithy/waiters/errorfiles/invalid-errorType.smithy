$version: "2.0"

namespace smithy.example

use smithy.waiters#waitable

@waitable(
    A: {
        "documentation": "A",
        "acceptors": [
            {
                "state": "success",
                "matcher": {
                    "errorType": "Nope"
                }
            }
        ]
    }
)
operation A {
    input: AInput,
    output: AOutput,
    errors: [OhNo],
}

@input
structure AInput {}

@output
structure AOutput {
    foo: String,
}

@error("client")
structure OhNo {}
