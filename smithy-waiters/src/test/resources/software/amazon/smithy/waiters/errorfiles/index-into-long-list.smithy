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
                        "path": "items[19].name",
                        "comparator": "stringEquals",
                        "expected": "x"
                    }
                }
            },
            {
                "state": "failure",
                "matcher": {
                    "output": {
                        "path": "items[19].name",
                        "comparator": "stringEquals",
                        "expected": "bad"
                    }
                },
                "message": "items[19].name"
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
    items: Items,
}

@length(min: 20)
list Items {
    member: Item
}

structure Item {
    name: String,
}
