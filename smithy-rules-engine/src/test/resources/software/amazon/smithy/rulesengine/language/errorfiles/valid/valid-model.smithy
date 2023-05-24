$version: "1.0"

namespace example

use smithy.rules#contextParam
use smithy.rules#endpointRuleSet
use smithy.rules#staticContextParams

@endpointRuleSet({
    "version": "1.3",
    "parameters": {
        "Region": {
            "type": "string",
            "builtIn": "AWS::Region",
            "documentation": "docs"
        },
        "ParameterFoo": {
            "type": "string",
            "documentation": "docs"
        },
        "ParameterBar": {
            "type": "string",
            "documentation": "docs"
        },
        "ExtraParameter": {
            "type": "string",
            "documentation": "docs"
        }
    },
    "rules": []
})
service FizzBuzz {
 operations: [GetResource, GetAnotherResource]
}

@staticContextParams(
  "ParameterFoo": {value: "foo"},
  "ExtraParameter": {value: "someValue"}
)
operation GetResource {
 input: GetResourceInput
}

structure GetResourceInput {
 @contextParam(name: "ParameterBar")
 ResourceId: ResourceId
}

@staticContextParams(
    "ParameterFoo": {value: "bar"}
)
operation GetAnotherResource {
    input: GetAnotherResourceInput
}

structure GetAnotherResourceInput {
    @contextParam(name: "ParameterBar")
    ResourceId: ResourceId
}

string ResourceId
