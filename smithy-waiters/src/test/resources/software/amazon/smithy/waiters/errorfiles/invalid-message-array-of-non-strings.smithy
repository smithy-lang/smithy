$version: "2.0"

namespace smithy.example

use smithy.waiters#waitable

@waitable(
    A: {
        "acceptors": [
            {
                "state": "success",
                "matcher": {
                    "output": {
                        "path": "status == 'ready'",
                        "comparator": "booleanEquals",
                        "expected": "true"
                    }
                }
            },
            {
                "state": "failure",
                "matcher": {
                    "output": {
                        "path": "status == 'failed'",
                        "comparator": "booleanEquals",
                        "expected": "true"
                    }
                },
                "message": "errorCodes"
            }
        ]
    }
)
operation A {
    input: AInput,
    output: AOutput,
}

@input
structure AInput {}

@output
structure AOutput {
    status: String,
    errorCodes: IntegerList,
}

list IntegerList {
    member: Integer,
}
