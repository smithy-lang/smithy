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
                        "path": "||",
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
                        // Note that this trips up the return type analysis too,
                        // but I want to make sure passing `10` to length is
                        // detected as an error.
                        "path": "length(`10`)",
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
