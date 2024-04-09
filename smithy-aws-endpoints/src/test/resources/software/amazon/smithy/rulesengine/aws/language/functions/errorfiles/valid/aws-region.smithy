$version: "2.0"

namespace example

use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@endpointRuleSet({
    "parameters": {
        "Region": {
            "type": "string",
            "builtIn": "AWS::Region",
            "documentation": "The region to dispatch this request, eg. `us-east-1`."
        }
    },
    "rules": [
        {
            "documentation": "Template the region into the URI when region is set",
            "conditions": [
                {
                    "fn": "isSet",
                    "argv": [
                        {
                            "ref": "Region"
                        }
                    ]
                }
            ],
            "endpoint": {
                "url": "https://{Region}.amazonaws.com",
                "properties": {
                    "authSchemes": [
                        {
                            "name": "sigv4",
                            "signingName": "serviceName",
                            "signingRegion": "{Region}",
                            "disableDoubleEncoding": false,
                            "disableNormalizePath": false,
                        }
                    ]
                }
            },
            "type": "endpoint"
        },
        {
            "documentation": "fallback when region is unset",
            "conditions": [],
            "error": "Region must be set to resolve a valid endpoint",
            "type": "error"
        }
    ],
    "version": "1.3"
})
@endpointTests(
    version: "1.0",
    testCases: [
        {
            "documentation": "basic region templating",
            "params": {
                "Region": "us-east-1"
            },
            "expect": {
                "endpoint": {
                    "url": "https://us-east-1.amazonaws.com",
                    "properties": {
                        "authSchemes": [
                            {
                                "name": "sigv4",
                                "signingRegion": "us-east-1",
                                "signingName": "serviceName"
                                "disableDoubleEncoding": false,
                                "disableNormalizePath": false,
                            }
                        ]
                    }
                }
            }
        },
        {
            "documentation": "test case where region is unset",
            "params": {},
            "expect": {
                "error": "Region must be set to resolve a valid endpoint"
            }
        }
    ]
)
service FizzBuzz {}
