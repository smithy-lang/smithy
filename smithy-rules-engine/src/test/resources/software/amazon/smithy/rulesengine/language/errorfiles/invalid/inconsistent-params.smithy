$version: "1.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#contextParam
use smithy.rules#endpointRuleSet
use smithy.rules#operationContextParams
use smithy.rules#staticContextParams

@endpointRuleSet({
    "version": "1.3",
    "parameters": {
        "Region": {
            "type": "string",
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
        },
        "ExtraBuiltIn": {
            "type": "string",
            "documentation": "docs",
            "builtIn": "SDK::Endpoint"
        },
        "StringArrayParam": {
            "type": "stringArray",
            "documentation": "docs"
        }
    },
    "rules": []
})
@clientContextParams(
    Region: {type: "string", documentation: "docs"},
    ExtraBuiltIn: {type: "string", documentation: "docs"}
)
service FizzBuzz {
    operations: [GetResource, GetAnotherResource]
}

@staticContextParams(
    "ParameterFoo": {value: true},
    "ParamNotInRuleset": {value: "someValue"},
    "InconsistentParamType": {value: true}
)
@operationContextParams(
    "StringArrayParam": {path: "ResourceId"},
    "InconsistentOperactionContextParam": {path: "ResourceId"}
)
operation GetResource {
    input: GetResourceInput
}

structure GetResourceInput {
    @contextParam(name: "ParameterBar")
    ResourceId: ResourceId
}

@staticContextParams(
    "ParameterFoo": {value: false},
    "ParamNotInRuleset": {value: "someOtherValue"},
    "InconsistentParamType": {value: "someValue"}
)
@operationContextParams(
    "InconsistentOperactionContextParam": {path: "ListOfStrings[*]"}
)
operation GetAnotherResource {
    input: GetAnotherResourceInput
}

structure GetAnotherResourceInput {
    @contextParam(name: "AnotherParameterBar")
    ResourceId: ResourceId,
    ListOfStrings: ListOfStrings
}

list ListOfStrings {
    member: String
}

string ResourceId
