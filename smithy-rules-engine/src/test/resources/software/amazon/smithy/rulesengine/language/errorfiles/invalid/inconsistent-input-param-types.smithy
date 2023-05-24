$version: "1.0"

namespace example

use smithy.rules#contextParam
use smithy.rules#endpointRuleSet

@endpointRuleSet({
    "version": "1.3",
    "parameters": {
        "ParameterBar": {
            "type": "String",
            "required": true,
            "documentation": "docs"
        }
    },
    "rules": [
        {
            "conditions": [],
            "documentation": "base rule",
            "endpoint": {
                "url": "https://{ParameterBar}.amazonaws.com",
                "headers": {}
            },
            "type": "endpoint"
        }
    ]
})
service FizzBuzz {
    operations: [GetResource, GetAnotherResource]
}

operation GetResource {
    input: GetResourceInput
}

structure GetResourceInput {
    @contextParam(name: "ParameterBar")
    ResourceId: ResourceId
}

operation GetAnotherResource {
    input: GetAnotherResourceInput
}

structure GetAnotherResourceInput {
    @contextParam(name: "ParameterBar")
    ResourceId: ResourceIdBool
}

string ResourceId
boolean ResourceIdBool
