$version: "2.0"

namespace example

use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@endpointRuleSet({
    "version": "1.1"
    "parameters": {}
    "rules": [
        {
            "documentation": "Limit 0",
            "conditions": [
                {
                    "fn": "split",
                    "argv": ["hi", "", 0]
                }
            ],
            "error": "Split cannot be empty"
            "type": "error"
        }
    ],
})
@endpointTests(
    version: "1.0",
    testCases: [
        {
            "documentation": "empty delimiter should throw IllegalArgumentException",
            "expect": {
                "error": "Split delimiter cannot be empty"
            }
        }
    ]
)
@suppress(["UnstableTrait.smithy"])
service SplitTestService {}
