$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@clientContextParams(
    bar: {type: "string", documentation: "a client string parameter"}
    baz: {type: "string", documentation: "another client string parameter"}
)
@endpointRuleSet({
    version: "1.0",
    parameters: {
        bar: {
            type: "string",
            documentation: "docs"
        }
        baz: {
            type: "string",
            documentation: "docs"
            required: true
            default: "baz"
        },
        endpoint: {
            type: "string",
            builtIn: "SDK::Endpoint",
            required: true,
            default: "asdf"
            documentation: "docs"
        },
    },
    rules: [
        {
            "documentation": "Template the region into the URI when FIPS is enabled",
            "conditions": [
                {
                    "fn": "isSet",
                    "argv": [
                        {
                            "ref": "bar"
                        }
                    ]
                }
            ],
            "endpoint": {
                "url": "https://example.com/{baz}"
            },
            "type": "endpoint"
        },
        {
            "conditions": [],
            "documentation": "error fallthrough",
            "error": "endpoint error",
            "type": "error"
        }
    ]
})
@endpointTests({
    "version": "1.0",
    "testCases": [
        {
            "params": {
                "bar": "a b",
            }
            "operationInputs": [{
                "operationName": "GetThing",
                "builtInParams": {
                    "SDK::Endpoint": "https://custom.example.com"
                }
            }],
            "expect": {
                "endpoint": {
                    "url": "https://example.com/baz"
                }
            }
        },
        {
            "params": {
                "bar": "a b",
                "baz": "BIG"
            }
            "operationInputs": [{
                "operationName": "GetThing",
                "builtInParams": {
                    "SDK::Endpoint": "https://custom.example.com"
                }
            }],
            "expect": {
                "endpoint": {
                    "url": "https://example.com/BIG"
                }
            }
        },
        {
            "documentation": "a documentation string",
            "expect": {
                "error": "endpoint error"
            }
        }
    ]
})
service FizzBuzz {
    version: "2022-01-01",
    operations: [GetThing]
}

operation GetThing {
    input := {}
}
