$version: "2.0"

namespace example

use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@endpointRuleSet({
  "parameters": {
    "Region": {
      "type": "string",
      "builtIn": "AWS::Region",
      "required": true,
      "documentation": "docs"
    }
  },
  "rules": [
    {
      "documentation": "override rule for the local pseduo region",
      "conditions": [
        {
          "fn": "stringEquals",
          "argv": [
            "local",
            "{Region}"
          ]
        }
      ],
      "endpoint": {
        "url": "http://localhost:8080"
      },
      "type": "endpoint"
    },
    {
      "documentation": "base rule",
      "conditions": [],
      "endpoint": {
        "url": "https://{Region}.someservice.amazonaws.com"
      },
      "type": "endpoint"
    }
  ],
  "version": "1.3"
})
@endpointTests(
  "version": "1.0",
  "testCases": [
    {
      "documentation": "local region override",
      "params": {
        "Region": "local"
      },
      "expect": {
        "endpoint": {
          "url": "http://localhost:8080"
        }
      }
    },
    {
      "documentation": "standard region templated",
      "params": {
        "Region": "us-east-2"
      },
      "expect": {
        "endpoint": {
          "url": "https://us-east-2.someservice.amazonaws.com"
        }
      }
    }
  ]
)
service FizzBuzz {}
