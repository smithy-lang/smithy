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
                        "path": "length(@)",
                        "comparator": "booleanEquals",
                        "expected": "true" // oops can't compare a number to a boolean
                    }
                }
            },
            {
                "state": "success",
                "matcher": {
                    "output": {
                        "path": "length(@)",
                        "comparator": "stringEquals",
                        "expected": "hi" // oops can't compare a number to a string
                    }
                }
            },
            {
                "state": "success",
                "matcher": {
                    "output": {
                        "path": "length(@)",
                        "comparator": "allStringEquals",
                        "expected": "hi" // oops can't compare a number to an array
                    }
                }
            },
            {
                "state": "success",
                "matcher": {
                    "output": {
                        "path": "length(@)",
                        "comparator": "anyStringEquals",
                        "expected": "hi" // oops can't compare a number to an array
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
