$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@clientContextParams(
    bar: {type: "string", documentation: "a client string parameter"}
    baz: {type: "string", documentation: "another client string parameter"}
)
@endpointRuleSet({
    version: "1.1",
    parameters: {
        bar: {
            type: "string",
            documentation: "docs"
        }
        baz: {
            type: "string",
            documentation: "docs"
        }
    },
    rules: [
        {
            "documentation": "Template baz into URI when bar is set"
            "conditions": [
                {
                    "fn": "coalesce"
                    "argv": [{"ref": "bar"}, {"ref": "baz"}, "oops"]
                    "assign": "hi"
                }
            ]
            "endpoint": {
                "url": "https://example.com/{hi}"
            }
            "type": "endpoint"
        }
    ]
})
@endpointTests({
    "version": "1.0",
    "testCases": [
        {
            "params": {
                "bar": "bar",
            }
            "operationInputs": [{
                "operationName": "GetThing"
            }],
            "expect": {
                "endpoint": {
                    "url": "https://example.com/bar"
                }
            }
        }
        {
            "params": {
                "baz": "baz"
            }
            "operationInputs": [{
                "operationName": "GetThing"
            }],
            "expect": {
                "endpoint": {
                    "url": "https://example.com/baz"
                }
            }
        }
        {
            "params": {}
            "operationInputs": [{
                "operationName": "GetThing"
            }],
            "expect": {
                "endpoint": {
                    "url": "https://example.com/oops"
                }
            }
        }
    ]
})
@suppress(["RuleSetParameter.TestCase.Unused"])
@suppress(["UnstableTrait.smithy"])
service FizzBuzz {
    version: "2022-01-01",
    operations: [GetThing]
}

operation GetThing {
    input := {}
}
