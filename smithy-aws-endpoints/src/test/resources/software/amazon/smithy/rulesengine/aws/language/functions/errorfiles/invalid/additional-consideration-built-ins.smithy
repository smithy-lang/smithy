$version: "1.0"

namespace example

use smithy.rules#endpointRuleSet

@endpointRuleSet({
    "version": "1.3",
    "parameters": {
        "Region": {
            "required": true,
            "builtIn": "AWS::Region",
            "type": "String",
            "documentation": "docs"
        },
        "AccountId": {
            "builtIn": "AWS::Auth::AccountId",
            "type": "String",
            "documentation": "docs"
        },
        "CredentialScope": {
            "builtIn": "AWS::Auth::CredentialScope",
            "type": "String",
            "documentation": "docs"
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
service FizzBuzz {}
