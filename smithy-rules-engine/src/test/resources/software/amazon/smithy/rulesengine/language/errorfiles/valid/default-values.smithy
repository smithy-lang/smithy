$version: "2.0"

namespace example

use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@endpointRuleSet({
  "parameters": {
    "Region": {
      "type": "string",
      "builtIn": "AWS::Region",
      "documentation": "The region to dispatch this request, eg. `us-east-1`.",
      "default": "us-west-5",
      "required": true
    },
    "UseFips": {
      "type": "boolean",
      "builtIn": "AWS::UseFIPS",
      "default": true,
      "required": true,
      "documentation": "docs"
    }
  },
  "rules": [
    {
      "documentation": "Template the region into the URI when FIPS is enabled",
      "conditions": [
        {
          "fn": "booleanEquals",
          "argv": [
            {
              "ref": "UseFips"
            },
            true
          ]
        }
      ],
      "endpoint": {
        "url": "https://fips.{Region}.amazonaws.com"
      },
      "type": "endpoint"
    },
    {
      "documentation": "error when fips is disabled",
      "conditions": [],
      "error": "UseFips = false",
      "type": "error"
    }
  ],
  "version": "1.3"
})
@endpointTests(
  "version": "1.0",
  "testCases": [
    {
      "documentation": "default endpoint",
      "params": {},
      "expect": {
        "endpoint": {
          "url": "https://fips.us-west-5.amazonaws.com"
        }
      }
    },
    {
      "documentation": "test case where FIPS is disabled",
      "params": {
        "UseFips": false
      },
      "expect": {
        "error": "UseFips = false"
      }
    },
    {
      "documentation": "test case where FIPS is enabled explicitly",
      "params": {
        "UseFips": true
      },
      "expect": {
        "endpoint": {
          "url": "https://fips.us-west-5.amazonaws.com"
        }
      }
    },
    {
      "documentation": "defaults can be overridden",
      "params": {
        "Region": "us-east-1"
      },
      "expect": {
        "endpoint": {
          "url": "https://fips.us-east-1.amazonaws.com"
        }
      }
    }
  ]
)
service FizzBuzz {}
