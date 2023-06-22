$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@suppress(["RuleSetParameter.Unused"])
@endpointRuleSet({
    "version": "1.3",
    "parameters": {
        "region": {
            "type": "string",
            "builtIn": "AWS::Region",
            "required": true,
            "documentation": "docs"
        },
        "useFIPSEndpoint": {
            "type": "boolean",
            "builtIn": "AWS::UseFIPS",
            "documentation": "docs"
        },
        "useDualStackEndpoint": {
            "type": "boolean",
            "builtIn": "AWS::UseDualStack",
            "documentation": "docs"
        }
        "endpointId": {
            "type": "string",
            "documentation": "docs"
        }
    },
    "rules": [
        {
            "conditions": [
                {
                    "fn": "aws.partition",
                    "argv": [
                        {
                            "ref": "region"
                        }
                    ],
                    "assign": "partitionResult"
                }
            ],
            "rules": [
                {
                    "conditions": [
                        {
                            "fn": "isSet",
                            "argv": [
                                {
                                    "ref": "endpointId"
                                }
                            ]
                        }
                    ],
                    "rules": [
                        {
                            "conditions": [
                                {
                                    "fn": "isSet",
                                    "argv": [
                                        {
                                            "ref": "useFIPSEndpoint"
                                        }
                                    ]
                                },
                                {
                                    "fn": "booleanEquals",
                                    "argv": [
                                        {
                                            "ref": "useFIPSEndpoint"
                                        },
                                        true
                                    ]
                                }
                            ],
                            "error": "FIPS endpoints not supported with multi-region endpoints",
                            "type": "error"
                        },
                        {
                            "conditions": [
                                {
                                    "fn": "not",
                                    "argv": [
                                        {
                                            "fn": "isSet",
                                            "argv": [
                                                {
                                                    "ref": "useFIPSEndpoint"
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "fn": "isSet",
                                    "argv": [
                                        {
                                            "ref": "useDualStackEndpoint"
                                        }
                                    ]
                                },
                                {
                                    "fn": "booleanEquals",
                                    "argv": [
                                        {
                                            "ref": "useDualStackEndpoint"
                                        },
                                        true
                                    ]
                                }
                            ],
                            "endpoint": {
                                "url": "https://{endpointId}.endpoint.events.{partitionResult#dualStackDnsSuffix}",
                                "properties": {
                                    "authSchemes": [
                                        {
                                            "name": "sigv4a",
                                            "signingName": "events",
                                            "signingRegionSet": [
                                                "*"
                                            ]
                                        }
                                    ]
                                }
                            },
                            "type": "endpoint"
                        },
                        {
                            "conditions": [],
                            "endpoint": {
                                "url": "https://{endpointId}.endpoint.events.{partitionResult#dnsSuffix}",
                                "properties": {
                                    "authSchemes": [
                                        {
                                            "name": "sigv4a",
                                            "signingName": "events",
                                            "signingRegionSet": [
                                                "*"
                                            ]
                                        }
                                    ]
                                }
                            },
                            "type": "endpoint"
                        }
                    ],
                    "type": "tree"
                },
                {
                    "conditions": [
                        {
                            "fn": "isValidHostLabel",
                            "argv": [
                                {
                                    "ref": "region"
                                },
                                false
                            ]
                        }
                    ],
                    "rules": [
                        {
                            "conditions": [
                                {
                                    "fn": "isSet",
                                    "argv": [
                                        {
                                            "ref": "useFIPSEndpoint"
                                        }
                                    ]
                                },
                                {
                                    "fn": "booleanEquals",
                                    "argv": [
                                        {
                                            "ref": "useFIPSEndpoint"
                                        },
                                        true
                                    ]
                                },
                                {
                                    "fn": "not",
                                    "argv": [
                                        {
                                            "fn": "isSet",
                                            "argv": [
                                                {
                                                    "ref": "useDualStackEndpoint"
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ],
                            "endpoint": {
                                "url": "https://events-fips.{region}.{partitionResult#dnsSuffix}",
                                "properties": {
                                    "authSchemes": [
                                        {
                                            "name": "sigv4a",
                                            "signingName": "events",
                                            "signingRegionSet": [
                                                "*"
                                            ]
                                        }
                                    ]
                                }
                            },
                            "type": "endpoint"
                        },
                        {
                            "conditions": [
                                {
                                    "fn": "isSet",
                                    "argv": [
                                        {
                                            "ref": "useDualStackEndpoint"
                                        }
                                    ]
                                },
                                {
                                    "fn": "booleanEquals",
                                    "argv": [
                                        {
                                            "ref": "useDualStackEndpoint"
                                        },
                                        true
                                    ]
                                },
                                {
                                    "fn": "not",
                                    "argv": [
                                        {
                                            "fn": "isSet",
                                            "argv": [
                                                {
                                                    "ref": "useFIPSEndpoint"
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ],
                            "endpoint": {
                                "url": "https://events.{region}.{partitionResult#dualStackDnsSuffix}",
                                "properties": {
                                    "authSchemes": [
                                        {
                                            "name": "sigv4a",
                                            "signingName": "events",
                                            "signingRegionSet": [
                                                "*"
                                            ]
                                        }
                                    ]
                                }
                            },
                            "type": "endpoint"
                        },
                        {
                            "conditions": [
                                {
                                    "fn": "isSet",
                                    "argv": [
                                        {
                                            "ref": "useDualStackEndpoint"
                                        }
                                    ]
                                },
                                {
                                    "fn": "isSet",
                                    "argv": [
                                        {
                                            "ref": "useFIPSEndpoint"
                                        }
                                    ]
                                },
                                {
                                    "fn": "booleanEquals",
                                    "argv": [
                                        {
                                            "ref": "useDualStackEndpoint"
                                        },
                                        true
                                    ]
                                },
                                {
                                    "fn": "booleanEquals",
                                    "argv": [
                                        {
                                            "ref": "useFIPSEndpoint"
                                        },
                                        true
                                    ]
                                }
                            ],
                            "endpoint": {
                                "url": "https://events-fips.{region}.{partitionResult#dualStackDnsSuffix}",
                                "properties": {
                                    "authSchemes": [
                                        {
                                            "name": "sigv4a",
                                            "signingName": "events",
                                            "signingRegionSet": [
                                                "*"
                                            ]
                                        }
                                    ]
                                }
                            },
                            "type": "endpoint"
                        },
                        {
                            "conditions": [],
                            "endpoint": {
                                "url": "https://events.{region}.{partitionResult#dnsSuffix}"
                            },
                            "type": "endpoint"
                        }
                    ],
                    "type": "tree"
                },
                {
                    "conditions": [],
                    "error": "{region} is not a valid HTTP host-label",
                    "type": "error"
                }
            ],
            "type": "tree"
        }
    ]
})
@endpointTests(
    version: "1.0",
    testCases: [
        {
            "documentation": "simple region endpoint",
            "params": {
                "region": "us-east-1",
                "notDefined": "some-value"
            },
            "expect": {
                "endpoint": {
                    "url": "https://events.us-east-1.amazonaws.com"
                }
            }
        },
        {
            "documentation": "basic case of endpointId",
            "params": {
                "region": "us-east-1",
                "endpointId": "myendpoint",
                "useDualStackEndpoint": false
            },
            "expect": {
                "endpoint": {
                    "url": "https://myendpoint.endpoint.events.amazonaws.com",
                    "properties": {
                        "authSchemes": [
                            {
                                "name": "sigv4a",
                                "signingName": "events",
                                "signingRegionSet": ["*"]
                            }
                        ]
                    }
                }
            }
        },
        {
            "documentation": "endpointId & FIPS",
            "params": {
                "region": "us-east-1",
                "endpointId": "myendpoint",
                "useFIPSEndpoint": true
            },
            "expect": {
                "error": "FIPS endpoints not supported with multi-region endpoints"
            }
        }
    ]
)
@clientContextParams(
    endpointId: {type: "string", documentation: "docs"}
)
service FizzBuzz {}
