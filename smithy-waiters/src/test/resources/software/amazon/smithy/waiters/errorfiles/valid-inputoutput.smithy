$version: "2.0"

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

@input
structure AInput {
    foo: String,
}

@output
structure AOutput {
    baz: String,
}
