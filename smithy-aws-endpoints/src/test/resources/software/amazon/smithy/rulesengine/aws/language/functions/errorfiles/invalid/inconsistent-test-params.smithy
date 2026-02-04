$version: "1.0"

namespace example

use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@endpointRuleSet({
    "version": "1.3",
    "parameters": {
        "Region": {
            "required": true,
            "builtIn": "AWS::Region",
            "type": "String",
            "documentation": "region",
        },
        "AccountId": {
            "builtIn": "AWS::Auth::AccountId",
            "type": "String",
            "documentation": "accountid",
        }
    },
    "rules": [
        {
            "conditions": [],
            "documentation": "base rule",
            "endpoint": {
                "url": "https://{Region}.fizzbuzz.amazonaws.com",
                "headers": {}
            },
            "type": "endpoint"
        }
    ]
})
@endpointTests(
    version: "1.3",
    testCases: [
        {
            "documentation": "Inconsistent Region and AccountId",
            "params": {
                "Region": "us-west-2",
                "AccountId": "123",
            },
            "expect": {
                "endpoint": {
                    "url": "https://us-west-2.fizzbuzz.amazonaws.com"
                }
            },
            "operationInputs": [
                {
                    "operationName": "ListShards",
                    "builtInParams": {
                        "AWS::Region": "us-west-1",
                        "AWS::Auth::AccountId": "012345678901",
                    }
                }
            ]
        }
        {
            "documentation": "Consistent Region and AccountId",
            "params": {
                "Region": "us-west-2",
                "AccountId": "012345678901",
            },
            "expect": {
                "endpoint": {
                    "url": "https://us-west-2.fizzbuzz.amazonaws.com"
                }
            },
            "operationInputs": [
                {
                    "operationName": "ListShards",
                    "builtInParams": {
                        "AWS::Region": "us-west-2",
                        "AWS::Auth::AccountId": "012345678901",
                    }
                }
            ]
        }
    ]
)
service FizzBuzz {
    operations: [
        ListShards
    ]
}

operation ListShards {
    input: Struct
}

structure Struct {}
