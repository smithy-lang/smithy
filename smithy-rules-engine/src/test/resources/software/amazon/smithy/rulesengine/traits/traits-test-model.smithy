$version: "1.0"

namespace smithy.example

use smithy.rules#clientContextParams
use smithy.rules#staticContextParams
use smithy.rules#contextParam
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@clientContextParams(
    stringFoo: {type: "string", documentation: "a client string parameter"},
    boolFoo: {type: "boolean", documentation: "a client boolean parameter"}
)
service ExampleService {
    version: "2022-01-01",
    operations: [GetThing]
}

apply ExampleService @endpointRuleSet({
    version: "1.0",
    parameters: {
        stringFoo: {type: "string"},
        stringBar: {type: "string"},
        stringBaz: {type: "string"},
        endpoint: {type: "string", builtIn: "SDK::Endpoint"},
        boolFoo: {type: "boolean"},
        boolBar: {type: "boolean"},
        boolBaz: {type: "string"}
    },
    rules: []
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
                "operationName": GetThing,
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
                    "properties": {
                        "authSchemes": [
                            {
                                "name": "v4",
                                "signingName": "example",
                                "signingScope": "us-west-2"
                            }
                        ]
                    },
                    "headers": {
                        "single": ["foo"],
                        "multi": ["foo", "bar", "baz"]
                    }
                }
            }
        }
    ]
})

@readonly
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
