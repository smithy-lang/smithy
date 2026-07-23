$version: "1.0"

namespace smithy.example

use smithy.rules#clientContextParams
use smithy.rules#contextParam
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@clientContextParams(
    Region: {type: "string", documentation: "The AWS region"}
)
service ImplicitDefaultService {
    version: "2022-01-01",
    operations: [OperationA, OperationB]
}

apply ImplicitDefaultService @endpointRuleSet({
    version: "1.0",
    parameters: {
        Region: {type: "string", documentation: "The AWS region"},
        // Bound only by OperationA and has a default, so it is required and in scope without a
        // gating condition. Removing OperationA orphans it.
        DefaultArn: {type: "string", required: true, default: "default-arn", documentation: "A defaulted ARN"}
    },
    rules: [
        {
            "documentation": "Uses DefaultArn's default directly in the URL with no gating condition",
            "conditions": [
                {"fn": "isSet", "argv": [{"ref": "Region"}]},
                {"fn": "stringEquals", "argv": [{"ref": "Region"}, "arn-region"]}
            ],
            "endpoint": {"url": "https://{DefaultArn}.arn.example.com", "properties": {}, "headers": {}},
            "type": "endpoint"
        },
        {
            "documentation": "Default regional endpoint",
            "conditions": [
                {"fn": "isSet", "argv": [{"ref": "Region"}]}
            ],
            "endpoint": {"url": "https://service.{Region}.example.com", "properties": {}, "headers": {}},
            "type": "endpoint"
        },
        {
            "documentation": "Fallback error",
            "conditions": [],
            "error": "No endpoint could be resolved",
            "type": "error"
        }
    ]
})

apply ImplicitDefaultService @endpointTests({
    "version": "1.0",
    "testCases": [
        {
            "documentation": "Explicitly sets DefaultArn - pruned by the static param-name scan",
            "params": {"Region": "arn-region", "DefaultArn": "explicit-arn"},
            "expect": {"endpoint": {"url": "https://explicit-arn.arn.example.com"}}
        },
        {
            "documentation": "Implicitly depends on DefaultArn default - pruned only by dynamic evaluation",
            "params": {"Region": "arn-region"},
            "expect": {"endpoint": {"url": "https://default-arn.arn.example.com"}}
        },
        {
            "documentation": "Regional endpoint - survives, does not depend on DefaultArn",
            "params": {"Region": "us-east-1"},
            "expect": {"endpoint": {"url": "https://service.us-east-1.example.com"}}
        }
    ]
})

operation OperationA {
    input: OperationAInput
}

@input
structure OperationAInput {
    @contextParam(name: "DefaultArn")
    defaultArn: String
}

operation OperationB {
    input: OperationBInput
}

@input
structure OperationBInput {
    name: String
}
