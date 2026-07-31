$version: "1.0"

namespace smithy.example

use smithy.rules#clientContextParams
use smithy.rules#contextParam
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests
use smithy.rules#staticContextParams

@clientContextParams(
    Region: {type: "string", documentation: "The AWS region"}
)
service EndpointService {
    version: "2022-01-01",
    operations: [OperationA, OperationB]
}

apply EndpointService @endpointRuleSet({
    version: "1.0",
    parameters: {
        Region: {type: "string", documentation: "The AWS region"},
        OperationType: {type: "string", documentation: "Type of operation"},
        StreamARN: {type: "string", documentation: "The stream ARN"},
        endpoint: {type: "string", builtIn: "SDK::Endpoint", documentation: "Override endpoint"}
    },
    rules: [
        {
            "documentation": "Custom endpoint override",
            "conditions": [
                {
                    "fn": "isSet",
                    "argv": [{"ref": "endpoint"}]
                }
            ],
            "endpoint": {
                "url": "{endpoint}",
                "properties": {},
                "headers": {}
            },
            "type": "endpoint"
        },
        {
            "documentation": "Route based on OperationType",
            "conditions": [
                {
                    "fn": "isSet",
                    "argv": [{"ref": "OperationType"}]
                },
                {
                    "fn": "stringEquals",
                    "argv": [{"ref": "OperationType"}, "control"]
                }
            ],
            "endpoint": {
                "url": "https://control.example.com",
                "properties": {},
                "headers": {}
            },
            "type": "endpoint"
        },
        {
            "documentation": "Route based on StreamARN",
            "conditions": [
                {
                    "fn": "isSet",
                    "argv": [{"ref": "StreamARN"}]
                }
            ],
            "endpoint": {
                "url": "https://stream.example.com",
                "properties": {},
                "headers": {}
            },
            "type": "endpoint"
        },
        {
            "documentation": "Default regional endpoint",
            "conditions": [
                {
                    "fn": "isSet",
                    "argv": [{"ref": "Region"}]
                }
            ],
            "endpoint": {
                "url": "https://service.{Region}.example.com",
                "properties": {},
                "headers": {}
            },
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

apply EndpointService @endpointTests({
    "version": "1.0",
    "testCases": [
        {
            "documentation": "Custom endpoint override",
            "params": {
                "endpoint": "https://override.example.com"
            },
            "expect": {
                "endpoint": {
                    "url": "https://override.example.com"
                }
            }
        },
        {
            "documentation": "Default regional",
            "params": {
                "Region": "us-east-1"
            },
            "expect": {
                "endpoint": {
                    "url": "https://service.us-east-1.example.com"
                }
            }
        },
        {
            "documentation": "Control operation type routing",
            "params": {
                "Region": "us-east-1",
                "OperationType": "control"
            },
            "expect": {
                "endpoint": {
                    "url": "https://control.example.com"
                }
            }
        },
        {
            "documentation": "Stream ARN routing",
            "params": {
                "Region": "us-east-1",
                "StreamARN": "arn:aws:kinesis:us-east-1:123456789:stream/my-stream"
            },
            "expect": {
                "endpoint": {
                    "url": "https://stream.example.com"
                }
            }
        }
    ]
})

@staticContextParams(
    OperationType: {value: "control"}
)
operation OperationA {
    input: OperationAInput
}

@input
structure OperationAInput {
    @contextParam(name: "StreamARN")
    streamArn: String
}

operation OperationB {
    input: OperationBInput
}

@input
structure OperationBInput {
    name: String
}
