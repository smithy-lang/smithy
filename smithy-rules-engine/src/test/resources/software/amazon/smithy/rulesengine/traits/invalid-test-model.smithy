$version: "2.0"

namespace smithy.example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet

@endpointRuleSet({
    "version": "1.3",
    "parameters": {
        "Region": {
            "builtIn": "AWS::Region",
            "type": "String",
            "required": true,
            "default": "us-east-1",
            "documentation": "The region to dispatch this request, eg. `us-east-1`."
        },
        "Stage": {
            "type": "String",
            "required": false
        },
        "Endpoint": {
            "builtIn": "SDK::Endpoint",
            "type": "String",
            "required": false,
            "documentation": "Override the endpoint used to send this request"
        }
    },
    "rules": [
        {
            "conditions": [
                {
                    "fn": "isSet",
                    "argv": [
                        {
                            "ref": "Endpoint"
                        }
                    ]
                },
                {
                    "fn": "parseURL",
                    "argv": [
                        {
                            "ref": "Endpoint"
                        }
                    ],
                    "assign": "url"
                }
            ],
            "endpoint": {
                "url": {
                    "ref": "Endpoint"
                },
                "properties": {},
                "headers": {}
            },
            "type": "endpoint"
        },
        {
            "conditions": [
                {
                    "fn": "stringEquals",
                    "argv": [
                        {
                            "ref": "Stage"
                        },
                        "staging"
                    ]
                }
            ],
            "endpoint": {
                "url": "https://exampleservice.{Region}.staging.example.com/2023-01-01",
                "properties": {},
                "headers": {}
            },
            "type": "endpoint"
        },
        {
            "documentation": "Fallback to production endpoint and default region",
            "conditions": [],
            "endpoint": {
                "url": "https://exampleservice.{Region}.example.com/2023-01-01",
                "properties": {},
                "headers": {}
            },
            "type": "endpoint"
        }
    ]
})
@clientContextParams(
    Stage: {type: "string", documentation: "The endpoint stage used to contruct the hostname."}
)
service ExampleService {
    version: "2023-01-01"
    operations: [GetThing]
}

@readonly
operation GetThing {
    input: GetThingInput
}

@input
structure GetThingInput {
    fizz: String
}
