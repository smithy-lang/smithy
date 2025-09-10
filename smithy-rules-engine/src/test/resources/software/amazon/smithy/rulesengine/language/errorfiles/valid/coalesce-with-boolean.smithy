$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@clientContextParams(
    bar: {type: "boolean", documentation: "a boolean value"}
    baz: {type: "boolean", documentation: "another boolean value"}
)
@endpointRuleSet({
    version: "1.1",
    parameters: {
        bar: {
            type: "boolean",
            documentation: "docs"
        }
        baz: {
            type: "boolean",
            documentation: "docs"
        }
    },
    rules: [
        {
            "documentation": "Template qux into URI when bar is set"
            "conditions": [
                {
                    "fn": "coalesce"
                    "argv": [
                        {"ref": "bar"}
                        {"ref": "baz"}
                    ]
                }
            ]
            "endpoint": {
                "url": "https://example.com"
            }
            "type": "endpoint"
        }
        {
            "documentation": "Did not match"
            "conditions": []
            "error": "Did not match"
            "type": "error"
        }
    ]
})
@endpointTests({
    "version": "1.0",
    "testCases": [
        {
            "params": {
                "bar": false // not null, so don't even look at baz, and it's falsey condition, so go to error
                "baz": false
            }
            "operationInputs": [{
                "operationName": "GetThing"
            }],
            "expect": {
                "error": "Did not match"
            }
        }
        {
            "params": {
                "bar": true // true != null, pick this, then the condition is truthy, so endpoint
                "baz": false
            }
            "operationInputs": [{
                "operationName": "GetThing"
            }],
            "expect": {
                "endpoint": {
                    "url": "https://example.com"
                }
            }
        }
        {
            "params": {
                // bar: null -- skip null values in coalesce
                "baz": true // truthy, so resolve an endpoint
            }
            "operationInputs": [{
                "operationName": "GetThing"
            }],
            "expect": {
                "endpoint": {
                    "url": "https://example.com"
                }
            }
        }
        {
            "params": {
                "bar": true // not null, truthy, so get an endpoint
                // baz is null, but bar is not-null first
            }
            "operationInputs": [{
                "operationName": "GetThing"
            }],
            "expect": {
                "endpoint": {
                    "url": "https://example.com"
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
