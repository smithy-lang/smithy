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
                        "comparator": "booleanEquals",
                        "expected": "true"
                    }
                }
            }
        ]
    },
    B: {
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
operation A {}
