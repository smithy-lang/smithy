$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet

@endpointRuleSet({
    "version": "1.3",
    "parameters": {
        "Region": {
            "required": true,
            "type": "String",
            "documentation": "docs"
        }
    },
    "rules": [
        {
            "conditions": [],
            "documentation": "base rule",
            "endpoint": {
                "url": "https://{Region}.amazonaws.com",
                "properties": {
                    "authSchemes": [
                        {
                            "name": "beta-test",
                            "signingName": "serviceName",
                            "signingRegion": "{Region}",
                            "additionalField": "test"
                        }
                    ]
                },
                "headers": {}
            },
            "type": "endpoint"
        }
    ]
})
@clientContextParams(
    Region: {type: "string", documentation: "docs"}
)
service FizzBuzz {}
