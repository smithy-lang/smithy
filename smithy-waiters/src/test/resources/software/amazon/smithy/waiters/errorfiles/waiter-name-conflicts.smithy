namespace smithy.example

use smithy.waiters#waitable

service InvalidService {
    version: "2020-11-30",
    operations: [A, B],
}

@waitable(
    A: {
        "documentation": "A",
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
operation A {
    output: AOutput,
}

structure AOutput {
    foo: String,
}

@waitable(
    A: {
        "documentation": "A",
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
operation B {
    output: AOutput,
}
