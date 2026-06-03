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
    },
    C: {
        "acceptors": [
            {
                "state": "retry",
                "matcher": {
                    "output": {
                        "path": "foo == 'bye'",
                        "comparator": "booleanEquals",
                        "expected": "true"
                    }
                }
            },
            {
                "state": "success",
                "matcher": {
                    "output": {
                        "path": "!foo",
                        "comparator": "booleanEquals",
                        "expected": "true"
                    }
                }
            },
            {
                "state": "failure",
                "matcher": {
                    "errorType": "OhNo"
                }
            }
        ]
    },
    D: {
        "acceptors": [
            {
                "state": "success",
                "matcher": {
                    "errorType": OhNo
                }
            }
        ]
    },
    E: {
        "acceptors": [
            {
                "state": "success",
                "matcher": {
                    "output": {
                        "path": "[foo]",
                        "expected": "hi",
                        "comparator": "allStringEquals"
                    }
                }
            },
            {
                "state": "failure",
                "matcher": {
                    "output": {
                        "path": "[foo]",
                        "expected": "bye",
                        "comparator": "anyStringEquals"
                    }
                }
            }
        ]
    },
    F: {
        "deprecated": true,
        "tags": ["A", "B"],
        "acceptors": [
            {
                "state": "success",
                "matcher": {
                    "success": true
                }
            },
            {
                "state": "failure",
                "matcher": {
                    "success": false
                }
            }
        ]
    },
    G: {
        "documentation": "Waiter with a message on failure acceptor",
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
            },
            {
                "state": "failure",
                "matcher": {
                    "output": {
                        "path": "foo == 'failed'",
                        "comparator": "booleanEquals",
                        "expected": "true"
                    }
                },
                "message": "foo"
            }
        ]
    },
    H: {
        "documentation": "Waiter with a list message on failure acceptor",
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
            },
            {
                "state": "failure",
                "matcher": {
                    "output": {
                        "path": "foo == 'failed'",
                        "comparator": "booleanEquals",
                        "expected": "true"
                    }
                },
                "message": "reasons"
            }
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
    reasons: Reasons,
}

list Reasons {
    member: String
}

@error("client")
structure OhNo {}
