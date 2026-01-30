$version: "2.0"

namespace smithy.example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@clientContextParams(
    Region: {type: "string", documentation: "docs"}
)
@endpointRuleSet({
    "version": "1.1"
    "parameters": {
        "Region": {
            "required": true
            "type": "String"
            "documentation": "docs"
        }
    }
    "rules": [
        {
            "conditions": []
            "documentation": "base rule"
            "endpoint": {
                "url": "https://{Region}.amazonaws.com"
                "properties": {}
                "headers": {}
            }
            "type": "endpoint"
        }
    ]
})
@endpointTests({
    "version": "1.0"
    "testCases": [
        {
            "documentation": "example endpoint test"
            "expect": {
                "endpoint": {
                    "url": "https://example-region.amazonaws.com"
                }
            }
            "params": {
                Region: "example-region"
            }
        }
    ]
})
service ExampleService {}
