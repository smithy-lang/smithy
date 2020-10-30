namespace smithy.example

use smithy.waiters#waitable

@waitable(
    A: {
        "documentation": "A",
        "acceptors": [
            {
                "state": "success",
                "matcher": {
                    "input": {
                        "path": "foo == 'hi'",
                        "expected": "true",
                        "comparator": "booleanEquals"
                    }
                }
            },
            {
                "state": "success",
                "matcher": {
                    "input": {
                        "path": "[foo]",
                        "expected": "hi",
                        "comparator": "allStringEquals"
                    }
                }
            },
            {
                "state": "failure",
                "matcher": {
                    "input": {
                        "path": "[foo]",
                        "expected": "hi",
                        "comparator": "arrayEmpty"
                    }
                }
            },
        ]
    }
)
operation A {
    input: AInput
}

structure AInput {
    foo: String,
}
