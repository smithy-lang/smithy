$version: "2.0"

namespace smithy.example

use smithy.waiters#waitable

@waitable(
    thingNotExists: {
        "documentation": "Something",
        "acceptors": [
            {
                "state": "success",
                "matcher": {
                    "output": {
                        "path": "baz == 'hi'",
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

@input
structure AInput {}

@output
structure AOutput {
    baz: String,
}
