$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet

@clientContextParams(
    foo: {type: "string", documentation: "a client string parameter"}
)
@endpointRuleSet({
    "version": "1.0",
    "parameters": {
        "foo": {
            "type": "String",
            "documentation": "docs"
        }
    },
    "rules": [
        {
            "conditions": [
                {
                    "fn": "coalesce",
                    "argv": [
                        {"ref": "foo"},
                        ""
                    ],
                    "assign": "hi"
                }
            ],
            "documentation": "base rule",
            "endpoint": {
                "url": "https://{hi}.amazonaws.com",
                "headers": {}
            },
            "type": "endpoint"
        }
    ]
})
service FizzBuzz {
    operations: [GetResource]
}

operation GetResource {}
