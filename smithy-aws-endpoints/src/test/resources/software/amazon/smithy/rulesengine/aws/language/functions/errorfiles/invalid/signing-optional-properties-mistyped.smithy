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
            "documentation": "Show failing type validation for optional fields",
            "conditions": [
                {
                    "fn": "isSet",
                    "argv": [
                        {
                            "ref": "Region"
                        }
                    ]
                },
                {
                    "fn": "stringEquals",
                    "argv": [
                        {
                            "ref": "Region"
                        },
                        "a"
                    ]
                }
            ],
            "endpoint": {
                "url": "https://{Region}.amazonaws.com",
                "properties": {
                    "authSchemes": [
                        {
                            "name": "sigv4",
                            "signingName": 1,
                            "signingRegion": 1,
                            "disableDoubleEncoding": 1,
                            "disableNormalizePath": 1
                        }
                    ]
                }
            },
            "type": "endpoint"
        },
        {
            "documentation": "Show failing type validation for optional fields",
            "conditions": [
                {
                    "fn": "isSet",
                    "argv": [
                        {
                            "ref": "Region"
                        }
                    ]
                },
                {
                    "fn": "stringEquals",
                    "argv": [
                        {
                            "ref": "Region"
                        },
                        "b"
                    ]
                }
            ],
            "endpoint": {
                "url": "https://{Region}.amazonaws.com",
                "properties": {
                    "authSchemes": [
                        {
                            "name": "sigv4a",
                            "signingName": 1,
                            "signingRegionSet": 1,
                            "disableDoubleEncoding": 1,
                            "disableNormalizePath": 1
                        }
                    ]
                }
            },
            "type": "endpoint"
        },
        {
            "documentation": "Show non-empty requirement of signingRegionSet",
            "conditions": [
                {
                    "fn": "isSet",
                    "argv": [
                        {
                            "ref": "Region"
                        }
                    ]
                },
            ],
            "endpoint": {
                "url": "https://{Region}.amazonaws.com",
                "properties": {
                    "authSchemes": [
                        {
                            "name": "sigv4a",
                            "signingRegionSet": []
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
service FizzBuzz {}
