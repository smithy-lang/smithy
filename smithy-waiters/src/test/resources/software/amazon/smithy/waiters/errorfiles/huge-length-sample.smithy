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
                        "path": "data",
                        "comparator": "anyStringEquals",
                        "expected": "hi"
                    }
                }
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
    data: L1,
}

@length(min: 2000000000)
list L1 {
    member: L2
}

@length(min: 2000000000)
list L2 {
    member: L3
}

@length(min: 2000000000)
list L3 {
    member: String
}
