namespace smithy.example

use smithy.waiters#waitable

@waitable(
    Bad: {
        "documentation": "A",
        "minDelay": 10,
        "maxDelay": 5,
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
            }
        ]
    },
    Good: {
        "minDelay": 5,
        "maxDelay": 10,
        "acceptors": [
            {
                "state": "success",
                "matcher": {
                    "output": {
                        "path": "foo == 'hey'",
                        "comparator": "booleanEquals",
                        "expected": "true"
                    }
                }
            }
        ]
    }
)
operation A {
    output: AOutput
}

structure AOutput {
    foo: String,
}
