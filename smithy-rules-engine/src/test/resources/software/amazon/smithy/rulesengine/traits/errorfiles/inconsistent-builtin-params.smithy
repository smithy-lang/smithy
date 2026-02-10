$version: "1.0"

namespace example

use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@endpointRuleSet({
  "version": "1.3",
  parameters: {
    endpoint: {
      type: "string"
      builtIn: "SDK::Endpoint"
      documentation: "docs"
    }
  },
  "rules": [
    {
      "conditions": [],
      "documentation": "base rule",
      "endpoint": {
        "url": "https://fizzbuzz.amazonaws.com",
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
      "documentation": "Inconsistent",
      "params": {
        endpoint: "https://another.example.com"
      },
      "expect": {
        "endpoint": {
          "url": "https://fizzbuzz.amazonaws.com"
        }
      },
      "operationInputs": [
        {
          "operationName": "ListShards",
          "builtInParams": {
            "SDK::Endpoint": "https://custom.example.com",
          }
        }
      ]
    }
    {
      "documentation": "Consistent",
      "params": {
        endpoint: "https://another.example.com"
      },
      "expect": {
        "endpoint": {
          "url": "https://fizzbuzz.amazonaws.com"
        }
      },
      "operationInputs": [
        {
          "operationName": "ListShards",
          "builtInParams": {
            "SDK::Endpoint": "https://another.example.com",
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
