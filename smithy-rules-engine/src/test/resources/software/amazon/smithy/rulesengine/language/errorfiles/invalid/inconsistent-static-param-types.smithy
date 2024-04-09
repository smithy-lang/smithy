$version: "1.0"

namespace example

use smithy.rules#endpointRuleSet
use smithy.rules#staticContextParams

@endpointRuleSet({
    "version": "1.3",
    "parameters": {
        "InconsistentParamType": {
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
                "url": "https://{InconsistentParamType}.amazonaws.com",
                "headers": {}
            },
            "type": "endpoint"
        }
    ]
})
service FizzBuzz {
 operations: [GetResource, GetAnotherResource]
}

@staticContextParams(
  "InconsistentParamType": {value: true}
)
operation GetResource {
 input: GetResourceInput
}

structure GetResourceInput {
 ResourceId: ResourceId
}

@staticContextParams(
    "InconsistentParamType": {value: "some-string"}
)
operation GetAnotherResource {
    input: GetAnotherResourceInput
}

structure GetAnotherResourceInput {
    ResourceId: ResourceId
}

string ResourceId
