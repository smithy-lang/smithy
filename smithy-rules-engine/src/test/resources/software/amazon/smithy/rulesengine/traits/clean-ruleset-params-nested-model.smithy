$version: "1.0"

namespace smithy.example

use smithy.rules#clientContextParams
use smithy.rules#contextParam
use smithy.rules#endpointRuleSet
use smithy.rules#staticContextParams

@clientContextParams(
    Region: {type: "string", documentation: "The AWS region"}
)
service NestedEndpointService {
    version: "2022-01-01",
    operations: [OperationA, OperationB]
}

apply NestedEndpointService @endpointRuleSet({
    version: "1.0",
    parameters: {
        Region: {type: "string", documentation: "The AWS region"},
        Bucket: {type: "string", documentation: "The bucket name"},
        StreamARN: {type: "string", documentation: "The stream ARN"},
        OperationType: {type: "string", documentation: "Type of operation"},
        endpoint: {type: "string", builtIn: "SDK::Endpoint", documentation: "Override endpoint"}
    },
    rules: [
        {
            "documentation": "Custom endpoint override",
            "conditions": [
                {"fn": "isSet", "argv": [{"ref": "endpoint"}]}
            ],
            "endpoint": {"url": "{endpoint}", "properties": {}, "headers": {}},
            "type": "endpoint"
        },
        {
            "documentation": "Region gate (level 1)",
            "conditions": [
                {"fn": "isSet", "argv": [{"ref": "Region"}]}
            ],
            "type": "tree",
            "rules": [
                {
                    "documentation": "Bucket gate (level 2)",
                    "conditions": [
                        {"fn": "isSet", "argv": [{"ref": "Bucket"}]}
                    ],
                    "type": "tree",
                    "rules": [
                        {
                            "documentation": "Bucket value gate (level 3)",
                            "conditions": [
                                {"fn": "stringEquals", "argv": [{"ref": "Bucket"}, "special"]}
                            ],
                            "type": "tree",
                            "rules": [
                                {
                                    "documentation": "Deep StreamARN leaf (level 4) - orphaned after OperationA removal",
                                    "conditions": [
                                        {"fn": "isSet", "argv": [{"ref": "StreamARN"}]},
                                        {"fn": "stringEquals", "argv": [{"ref": "StreamARN"}, "arn:special"]}
                                    ],
                                    "endpoint": {"url": "https://stream.example.com", "properties": {}, "headers": {}},
                                    "type": "endpoint"
                                },
                                {
                                    "documentation": "Deep fallback leaf (level 4)",
                                    "conditions": [],
                                    "endpoint": {"url": "https://special.{Region}.example.com", "properties": {}, "headers": {}},
                                    "type": "endpoint"
                                }
                            ]
                        },
                        {
                            "documentation": "Bucket default (level 3)",
                            "conditions": [],
                            "endpoint": {"url": "https://bucket.{Region}.example.com", "properties": {}, "headers": {}},
                            "type": "endpoint"
                        }
                    ]
                },
                {
                    "documentation": "Collapsible subtree (level 2) - gated by Bucket, but ALL children reference OperationType",
                    "conditions": [
                        {"fn": "isSet", "argv": [{"ref": "Bucket"}]},
                        {"fn": "stringEquals", "argv": [{"ref": "Bucket"}, "routed"]}
                    ],
                    "type": "tree",
                    "rules": [
                        {
                            "documentation": "Control routing (level 3)",
                            "conditions": [
                                {"fn": "isSet", "argv": [{"ref": "OperationType"}]},
                                {"fn": "stringEquals", "argv": [{"ref": "OperationType"}, "control"]}
                            ],
                            "endpoint": {"url": "https://control.example.com", "properties": {}, "headers": {}},
                            "type": "endpoint"
                        },
                        {
                            "documentation": "Data routing (level 3)",
                            "conditions": [
                                {"fn": "isSet", "argv": [{"ref": "OperationType"}]},
                                {"fn": "stringEquals", "argv": [{"ref": "OperationType"}, "data"]}
                            ],
                            "endpoint": {"url": "https://data.example.com", "properties": {}, "headers": {}},
                            "type": "endpoint"
                        }
                    ]
                }
            ]
        },
        {
            "documentation": "Fallback error",
            "conditions": [],
            "error": "No endpoint could be resolved",
            "type": "error"
        }
    ]
})

@staticContextParams(
    Region: {value: "us-east-1"},
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
    @contextParam(name: "Bucket")
    bucket: String
}
