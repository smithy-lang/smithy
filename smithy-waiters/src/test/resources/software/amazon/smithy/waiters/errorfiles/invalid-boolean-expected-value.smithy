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
                    "output": {
                        "path": "`true`",
                        "comparator": "booleanEquals",
                        "expected": "foo" // must be true | false
                    }
                }
            },
            {
                "state": "retry",
                "matcher": {
                    "output": {
                        "path": "`true`",
                        "comparator": "booleanEquals",
                        "expected": "true" // this is fine
                    }
                }
            },
            {
                "state": "failure",
                "matcher": {
                    "output": {
                        "path": "`true`",
                        "comparator": "booleanEquals",
                        "expected": "false" // this is fine
                    }
                }
            },
        ]
    }
)
operation A {
    input: AInput,
    output: AOutput,
    errors: [OhNo],
}

@input
structure AInput {}

@output
structure AOutput {
    foo: String,
}

@error("client")
structure OhNo {}
