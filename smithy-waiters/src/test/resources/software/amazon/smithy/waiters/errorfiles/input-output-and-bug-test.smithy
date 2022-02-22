$version: "2.0"

namespace smithy.example

use smithy.waiters#waitable

@waitable(
    A: {
        documentation: "A",
        acceptors: [
            {
                state: "success",
                matcher: {
                    inputOutput: {
                        path: "(input.Status == 'failed') && (output.Status == 'failed')",
                        expected: "true",
                        comparator: "booleanEquals"
                    }
                }
            }
        ]
    }
)
operation WaitersTest {
    input: WaitersTestInputOutput,
    output: WaitersTestInputOutput
}

structure WaitersTestInputOutput {
    Status: String
}
