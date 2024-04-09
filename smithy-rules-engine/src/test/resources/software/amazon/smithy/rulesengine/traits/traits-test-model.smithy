$version: "1.0"

namespace smithy.example

use smithy.rules#clientContextParams
use smithy.rules#contextParam
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests
use smithy.rules#staticContextParams

@clientContextParams(
    stringFoo: {type: "string", documentation: "a client string parameter"},
    boolFoo: {type: "boolean", documentation: "a client boolean parameter"}
)
@suppress(["RuleSetParameter.TestCase.Unused"])
service ExampleService {
    version: "2022-01-01",
    operations: [GetThing, Ping]
}

apply ExampleService @endpointRuleSet({
    version: "1.0",
    parameters: {
        stringFoo: {type: "string", documentation: "docs"},
        stringBar: {type: "string", documentation: "docs"},
        stringBaz: {type: "string", documentation: "docs"},
        endpoint: {type: "string", builtIn: "SDK::Endpoint", documentation: "docs"},
        boolFoo: {type: "boolean", documentation: "docs"},
        boolBar: {type: "boolean", documentation: "docs"},
        boolBaz: {type: "string", documentation: "docs"}
    },
    rules: [
        {
            "documentation": "Template the region into the URI when FIPS is enabled",
            "conditions": [
                {
                    "fn": "isSet",
                    "argv": [
                        {
                            "ref": "boolFoo"
                        }
                    ]
                },
                {
                    "fn": "booleanEquals",
                    "argv": [
                        {
                            "ref": "boolFoo"
                        },
                        true
                    ]
                }
            ],
            "endpoint": {
                "url": "https://example.com",
                "properties": {},
                "headers": {
                    "single": ["foo"],
                    "multi": ["foo", "bar", "baz"]
                }
            },
            "type": "endpoint"
        },
        {
            "documentation": "error when boolFoo is false",
            "conditions": [
                {
                    "fn": "isSet",
                    "argv": [
                        {
                            "ref": "boolFoo"
                        }
                    ]
                },
                {
                    "fn": "booleanEquals",
                    "argv": [
                        {
                            "ref": "boolFoo"
                        },
                        false
                    ]
                }
            ],
            "error": "endpoint error",
            "type": "error"
        }
    ]
})

apply ExampleService @endpointTests({
    "version": "1.0",
    "testCases": [
        {
            "documentation": "a documentation string",
            "params": {
                "stringFoo": "a b",
                "boolFoo": false
            },
            "expect": {
                "error": "endpoint error"
            }
        },
        {
            "params": {
                "stringFoo": "c d",
                "boolFoo": true
            },
            "operationInputs": [{
                "operationName": "GetThing",
                "clientParams": {
                    "stringFoo": "client value"
                },
                "operationParams": {
                    "buzz": "a buzz value",
                    "fizz": "a required value"
                },
                "builtInParams": {
                    "SDK::Endpoint": "https://custom.example.com"
                }
            }],
            "expect": {
                "endpoint": {
                    "url": "https://example.com",
                    "properties": {},
                    "headers": {
                        "single": ["foo"],
                        "multi": ["foo", "bar", "baz"]
                    }
                }
            }
        }
    ]
})

@staticContextParams(
    stringBar: {value: "some value"},
    boolBar: {value: true}
)
operation GetThing {
    input: GetThingInput
}

@input
structure GetThingInput {
    @required
    fizz: String,

    @contextParam(name: "stringBaz")
    buzz: String,

    @contextParam(name: "boolBaz")
    fuzz: String,
}

operation Ping {}
