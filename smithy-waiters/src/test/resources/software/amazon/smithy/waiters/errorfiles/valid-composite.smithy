namespace smithy.example

use smithy.waiters#waitable

// These acceptors are somewhat nonsensical, but are just to assert that the
// composite loaders actually work.
@waitable(
    A: {
        "documentation": "A",
        "acceptors": [
            {
                "state": "success",
                "matcher": {
                    "not": {
                        "output": {
                            "path" : "foo == 'hi'",
                            "expected": "true",
                            "comparator": "booleanEquals"
                        }
                    }
                }
            },
            {
                "state": "success",
                "matcher": {
                    "and": [
                        {
                            "output": {
                                "path" : "foo == 'bye'",
                                "expected": "true",
                                "comparator": "booleanEquals"
                            }
                        },
                        {
                            "success": true
                        }
                    ]
                }
            },
            {
                "state": "success",
                "matcher": {
                    // These do the same thing, but this is just to test loading works.
                    "or": [
                        {
                            "not": {
                                "success": true
                            }
                        },
                        {
                            "success": false
                        }
                    ]
                }
            },
        ]
    }
)
operation A {
    output: AOutput
}

structure AOutput {
    foo: String,
}
