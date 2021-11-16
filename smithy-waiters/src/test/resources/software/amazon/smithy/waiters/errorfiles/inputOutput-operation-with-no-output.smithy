namespace smithy.example

use smithy.waiters#waitable

@waitable(
    A: {
        "documentation": "A",
        "acceptors": [
            {
                "state": "success",
                "matcher": {
                    "inputOutput": {
                        "path": "output.foo == 'hi'",
                        "expected": "true",
                        "comparator": "booleanEquals"
                    }
                }
            }
        ]
    }
)
@suppress(["OperationMissingOutput"])
operation A {
    input: AInput
}

@input
structure AInput {
    foo: String,
}
