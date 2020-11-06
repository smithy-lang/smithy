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
                        "path": "input.foo == output.baz",
                        "expected": "true",
                        "comparator": "booleanEquals"
                    }
                }
            }
        ]
    }
)
operation A {
    input: AInput,
    output: AOutput
}

structure AInput {
    foo: String,
}

structure AOutput {
    baz: String,
}
