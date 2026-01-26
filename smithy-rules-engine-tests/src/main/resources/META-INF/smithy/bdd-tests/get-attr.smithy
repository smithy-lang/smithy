$version: "2.0"

namespace smithy.tests.endpointrules.getattr

use smithy.rules#clientContextParams
use smithy.rules#endpointBdd
use smithy.rules#endpointTests

@suppress([
    "UnstableTrait"
])
@clientContextParams(
    Bucket: {
        type: "string"
        documentation: "docs"
    }
)
@endpointBdd(
    version: "1.1"
    parameters: {
        Bucket: {
            required: false
            documentation: "docs"
            type: "string"
        }
    }
    conditions: [
        {
            fn: "isSet"
            argv: [
                {
                    ref: "Bucket"
                }
            ]
        }
        {
            fn: "parseURL"
            argv: [
                "{Bucket}"
            ]
            assign: "bucketUrl"
        }
        {
            fn: "getAttr"
            argv: [
                {
                    ref: "bucketUrl"
                }
                "path"
            ]
            assign: "path"
        }
    ]
    results: [
        {
            conditions: []
            endpoint: {
                url: "https://{bucketUrl#authority}{path}"
                properties: {}
                headers: {}
            }
            type: "endpoint"
        }
        {
            documentation: "fallback when no tests match"
            conditions: []
            error: "No tests matched"
            type: "error"
        }
    ]
    root: 2
    nodeCount: 4
    nodes: "/////wAAAAH/////AAAAAAAAAAMF9eECAAAAAQAAAAQF9eECAAAAAgX14QEF9eEC"
)
@endpointTests(
    version: "1.0"
    testCases: [
        {
            documentation: "getAttr on top level values in function and template"
            params: {
                Bucket: "http://example.com/path"
            }
            expect: {
                endpoint: {
                    url: "https://example.com/path"
                }
            }
        }
    ]
)
service FizzBuzz {
}
