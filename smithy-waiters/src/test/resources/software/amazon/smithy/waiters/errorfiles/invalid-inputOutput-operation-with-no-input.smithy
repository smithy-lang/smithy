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
operation A {
    output: AOutput
}

structure AOutput {
    foo: String,
}
