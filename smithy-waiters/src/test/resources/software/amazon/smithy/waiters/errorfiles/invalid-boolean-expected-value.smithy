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
    output: AOutput,
    errors: [OhNo],
}

structure AOutput {
    foo: String,
}

@error("client")
structure OhNo {}
