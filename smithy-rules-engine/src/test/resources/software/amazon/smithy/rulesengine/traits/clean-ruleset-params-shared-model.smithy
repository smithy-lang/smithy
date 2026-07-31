$version: "1.0"

namespace smithy.example

use smithy.rules#contextParam
use smithy.rules#endpointRuleSet
use smithy.rules#staticContextParams

service SharedEndpointService {
    version: "2022-01-01",
    operations: [OperationA, OperationB]
}

apply SharedEndpointService @endpointRuleSet({
    version: "1.0",
    parameters: {
        SharedParam: {type: "string", documentation: "Bound by both operations"},
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
            "documentation": "Route based on SharedParam",
            "conditions": [
                {"fn": "isSet", "argv": [{"ref": "SharedParam"}]}
            ],
            "endpoint": {"url": "https://{SharedParam}.example.com", "properties": {}, "headers": {}},
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

@staticContextParams(
    SharedParam: {value: "operation-a"}
)
operation OperationA {
    input: OperationAInput
}

@input
structure OperationAInput {
    name: String
}

operation OperationB {
    input: OperationBInput
}

@input
structure OperationBInput {
    @contextParam(name: "SharedParam")
    sharedB: String
}
