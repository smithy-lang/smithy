$version: "2.0"

namespace smithy.rules.tests

use smithy.rules#clientContextParams
use smithy.rules#endpointBdd
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@suppress(["UnstableTrait"])
@endpointRuleSet({
    "version": "1.0",
    "parameters": {
        "Bucket": {
            "type": "string",
            "documentation": "docs"
        }
    },
    "rules": [
        {
            "documentation": "bucket is set, handle bucket specific endpoints",
            "conditions": [
                {
                    "fn": "isSet",
                    "argv": [
                        {
                            "ref": "Bucket"
                        }
                    ]
                },
                {
                    "fn": "parseURL",
                    "argv": [
                        "{Bucket}"
                    ],
                    "assign": "bucketUrl"
                },
                {
                    "fn": "getAttr",
                    "argv": [
                        {
                            "ref": "bucketUrl"
                        },
                        "path"
                    ],
                    "assign": "path"
                }
            ],
            "endpoint": {
                "url": "https://{bucketUrl#authority}{path}"
            },
            "type": "endpoint"
        }
    ]
})
@endpointTests({
    "version": "1.0",
    "testCases": [
        {
            "documentation": "getAttr on top level values in function and template"
            "params": {
                "Bucket": "http://example.com/path"
            }
            "expect": {
                "endpoint": {
                    "url": "https://example.com/path"
                }
            }
        }
    ]
})
@clientContextParams(
    Bucket: {type: "string", documentation: "docs"}
)
service GetAttrService {}
