$version: "2.0"

namespace smithy.example

use smithy.waiters#waitable

@waitable(
    A: {
        "acceptors": [
            {
                "state": "success",
                "matcher": {
                    "output": {
                        "path": "foo == 'hi'",
                        "comparator": "booleanEquals",
                        "expected": "true"
                    }
                }
            },
            {
                "state": "failure",
                "matcher": {
                    "output": {
                        "path": "foo == 'bad'",
                        "comparator": "booleanEquals",
                        "expected": "true"
                    }
                },
                "message": "baz"
            }
        ]
    }
)
operation A {
    input: AInput,
    output: AOutput,
}

@input
structure AInput {}

@output
structure AOutput {
    foo: String,
    baz: Boolean,
}
