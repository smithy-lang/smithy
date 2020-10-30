namespace smithy.example

use smithy.waiters#waitable

@waitable(
    thingNotExists: {
        "documentation": "Something",
        "acceptors": [
            {
                "state": "success",
                "matcher": {
                    "input": {
                        "path": "foo == 'hi'",
                        "comparator": "booleanEquals",
                        "expected": "true"
                    }
                }
            }
        ]
    }
)
operation A {
    input: AInput,
    output: AOutput,
}

structure AInput {
    foo: String,
}

structure AOutput {
    baz: String,
}
