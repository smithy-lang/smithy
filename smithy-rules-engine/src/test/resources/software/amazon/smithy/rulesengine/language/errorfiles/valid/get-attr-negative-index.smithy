$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@endpointRuleSet({
    "version": "1.3",
    "parameters": {
        "Input": {
            "type": "string",
            "required": true,
            "documentation": "The input string to split"
        }
    },
    "rules": [
        {
            "documentation": "Test negative index access",
            "conditions": [
                {
                    "fn": "split",
                    "argv": ["{Input}", "--", 0],
                    "assign": "parts"
                },
                {
                    "fn": "getAttr",
                    "argv": [{"ref": "parts"}, "[-1]"],
                    "assign": "last"
                },
                {
                    "fn": "getAttr",
                    "argv": [{"ref": "parts"}, "[-2]"],
                    "assign": "secondLast"
                }
            ],
            "endpoint": {
                "url": "https://example.com/{secondLast}/{last}"
            },
            "type": "endpoint"
        },
        {
            "documentation": "Fallback",
            "conditions": [],
            "error": "No input provided",
            "type": "error"
        }
    ]
})
@endpointTests({
    "version": "1.0",
    "testCases": [
        {
            "documentation": "S3Express bucket with multiple delimiters extracts AZ correctly",
            "params": {"Input": "my--s3--bucket--abcd-ab1--x-s3"},
            "expect": {
                "endpoint": {"url": "https://example.com/abcd-ab1/x-s3"}
            }
        },
        {
            "documentation": "Simple two-part split",
            "params": {"Input": "first--second"},
            "expect": {
                "endpoint": {"url": "https://example.com/first/second"}
            }
        }
    ]
})
@clientContextParams(
    Input: {type: "string", documentation: "The input string"}
)
service TestService {}
