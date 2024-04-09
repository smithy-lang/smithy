$version: "2.0"

namespace smithy.example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@clientContextParams(
    bar: {type: "string", documentation: "a client string parameter"}
)
@endpointRuleSet({
    version: "1.0",
    parameters: {
        bar: {type: "string", documentation: "docs"},
        endpoint: {type: "string", builtIn: "SDK::Endpointt", documentation: "docs"},
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
                "url": "https://example.com"
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
                    "SDK::Endpointt": "https://custom.example.com"
                }
            }],
            "expect": {
                "endpoint": {
                    "url": "https://example.com"
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
service ExampleService {
    version: "2022-01-01",
    operations: [GetThing]
}

operation GetThing {
    input := {}
}
