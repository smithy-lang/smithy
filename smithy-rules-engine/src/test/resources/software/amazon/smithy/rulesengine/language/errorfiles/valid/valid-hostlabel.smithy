$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@endpointRuleSet({
  "parameters": {
    "Region": {
      "type": "string",
      "required": true,
      "documentation": "The region to dispatch this request, eg. `us-east-1`."
    }
  },
  "rules": [
    {
      "documentation": "Template the region into the URI when region is set",
      "conditions": [
        {
          "fn": "isValidHostLabel",
          "argv": [
            {
              "ref": "Region"
            },
            false
          ]
        }
      ],
      "endpoint": {
        "url": "https://{Region}.amazonaws.com"
      },
      "type": "endpoint"
    },
    {
      "documentation": "Template the region into the URI when region is set",
      "conditions": [
        {
          "fn": "isValidHostLabel",
          "argv": [
            {
              "ref": "Region"
            },
            true
          ]
        }
      ],
      "endpoint": {
        "url": "https://{Region}-subdomains.amazonaws.com"
      },
      "type": "endpoint"
    },
    {
      "documentation": "Region was not a valid host label",
      "conditions": [],
      "error": "Invalid hostlabel",
      "type": "error"
    }
  ],
  "version": "1.3"
})
@endpointTests(
  version: "1.0",
  testCases: [
    {
      "documentation": "standard region is a valid hostlabel",
      "params": {
        "Region": "us-east-1"
      },
      "expect": {
        "endpoint": {
          "url": "https://us-east-1.amazonaws.com"
        }
      }
    },
    {
      "documentation": "starting with a number is a valid hostlabel",
      "params": {
        "Region": "3aws4"
      },
      "expect": {
        "endpoint": {
          "url": "https://3aws4.amazonaws.com"
        }
      }
    },
    {
      "documentation": "when there are dots, only match if subdomains are allowed",
      "params": {
        "Region": "part1.part2"
      },
      "expect": {
        "endpoint": {
          "url": "https://part1.part2-subdomains.amazonaws.com"
        }
      }
    },
    {
      "documentation": "a space is never a valid hostlabel",
      "params": {
        "Region": "part1 part2"
      },
      "expect": {
        "error": "Invalid hostlabel"
      }
    },
    {
      "documentation": "an empty string is not a valid hostlabel",
      "params": {
        "Region": ""
      },
      "expect": {
        "error": "Invalid hostlabel"
      }
    },
    {
      "documentation": "ending with a dot is not a valid hostlabel",
      "params": {
        "Region": "part1."
      },
      "expect": {
        "error": "Invalid hostlabel"
      }
    },
    {
      "documentation": "multiple consecutive dots are not allowed",
      "params": {
        "Region": "part1..part2"
      },
      "expect": {
        "error": "Invalid hostlabel"
      }
    },
    {
      "documentation": "labels cannot start with a dash",
      "params": {
        "Region": "part1.-part2"
      },
      "expect": {
        "error": "Invalid hostlabel"
      }
    }
  ]
)
@clientContextParams(
  Region: {type: "string", documentation: "docs"}
)
service FizzBuzz {}
