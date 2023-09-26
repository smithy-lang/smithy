$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@clientContextParams(
    bar: {type: "string", documentation: "a client string parameter"}
)
@endpointRuleSet({
    version: "1.0",
    parameters: {
        bar: {
            type: "string",
            documentation: "docs"
        }
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
            "documentation": "Shows invalid auth without a signing name",
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
                "url": "https://example.com/",
                "properties": {
                    "authSchemes": [
                        {}
                    ]
                }
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
service FizzBuzz {
    version: "2022-01-01",
    operations: [GetThing]
}

operation GetThing {
    input := {}
}
