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
    output: AOutput,
    errors: [OhNo],
}

structure AOutput {
    foo: String,
}

@error("client")
structure OhNo {}
