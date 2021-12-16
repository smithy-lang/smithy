$version: "2.0"

namespace smithy.example

use smithy.waiters#waitable

@waitable(
    MissingSuccessState: {
        "documentation": "This waiter is missing a success state",
        "acceptors": [
            {
                "state": "failure",
                "matcher": {
                    "success": true
                }
            }
        ]
    }
)
operation A {
    input: AInput,
    output: AOutput
}

@input
structure AInput {}

@output
structure AOutput {}
