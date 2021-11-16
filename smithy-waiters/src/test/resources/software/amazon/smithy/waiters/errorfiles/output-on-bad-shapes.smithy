namespace smithy.example

use smithy.waiters#waitable

@waitable(
    A: {
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
@suppress(["OperationMissingInput", "OperationMissingOutput"])
operation A {}
