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
    input: AInputOutput,
    output: AInputOutput
}

structure AInputOutput {}
