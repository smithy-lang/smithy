$version: "2.0"

namespace smithy.example

use smithy.waiters#waitable

@waitable(
    Invalid1: {
        "documentation": "A",
        "acceptors": [
            {
                "state": "success",
                "matcher": {
                    "output": {
                        "path": "`10`.foo", // can't select a field from a literal.
                        "comparator": "booleanEquals",
                        "expected": "true"
                    }
                }
            }
        ]
    },
    Invalid2: {
        "acceptors": [
            {
                "state": "success",
                "matcher": {
                    "output": {
                        "path": "`true` < `false`", // can't compare these
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
    output: AOutput
}

@input
structure AInput {}

@output
structure AOutput {
    foo: String,
}
