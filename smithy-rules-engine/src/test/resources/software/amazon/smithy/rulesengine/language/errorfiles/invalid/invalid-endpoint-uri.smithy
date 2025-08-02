$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet

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
    },
    rules: [
        {
            "documentation": "lorem ipsum dolor",
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
            "type": "endpoint",
            "endpoint": {
                "url": "foo://example.com/"
            }
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
    version: "2022-01-01"
}
