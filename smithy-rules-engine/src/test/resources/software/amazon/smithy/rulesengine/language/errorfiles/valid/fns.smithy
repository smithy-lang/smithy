$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@endpointRuleSet({
  "documentation": "functions in more places",
  "parameters": {
    "Uri": {
      "type": "string",
      "documentation": "A URI to use"
    },
    "Arn": {
      "type": "string",
      "documentation": "an ARN to extract fields from"
    },
    "CustomError": {
      "type": "string",
      "documentation": "when set, a custom error message"
    }
  },
  "rules": [
    {
      "documentation": "when URI is set, use it directly",
      "conditions": [
        {
          "fn": "isSet",
          "argv": [
            {
              "ref": "Uri"
            }
          ]
        },
        {
          "fn": "isSet",
          "argv": [
            {
              "ref": "Arn"
            }
          ]
        },
        {
          "fn": "aws.parseArn",
          "argv": [
            {
              "ref": "Arn"
            }
          ],
          "assign": "parsedArn"
        }
      ],
      "endpoint": {
        "url": {
          "ref": "Uri"
        },
        "headers": {
          "x-uri": [
            {
              "ref": "Uri"
            }
          ],
          "x-arn-region": [
            {
              "fn": "getAttr",
              "argv": [
                {
                  "ref": "parsedArn"
                },
                "region"
              ]
            }
          ]
        }
      },
      "type": "endpoint"
    },
    {
      "documentation": "A custom error",
      "conditions": [
        {
          "fn": "isSet",
          "argv": [
            {
              "ref": "CustomError"
            }
          ]
        }
      ],
      "type": "error",
      "error": {
        "ref": "CustomError"
      }
    },
    {
      "type": "error",
      "conditions": [
        {
          "fn": "isSet",
          "argv": [
            {
              "ref": "Arn"
            }
          ]
        },
        {
          "fn": "aws.parseArn",
          "argv": [
            {
              "ref": "Arn"
            }
          ],
          "assign": "parsedArn"
        }
      ],
      "error": {
        "fn": "getAttr",
        "argv": [
          {
            "ref": "parsedArn"
          },
          "partition"
        ]
      }
    },
    {
      "documentation": "fallback when nothing is set",
      "conditions": [],
      "error": "No fields were set",
      "type": "error"
    }
  ],
  "version": "1.3"
})
@endpointTests(
  "version": "1.0",
  "testCases": [
    {
      "documentation": "test where URI is set and flows to URI and header",
      "params": {
        "Uri": "https://www.example.com",
        "Arn": "arn:aws:s3:us-east-2:012345678:outpost:op-1234"
      },
      "expect": {
        "endpoint": {
          "url": "https://www.example.com",
          "headers": {
            "x-uri": [
              "https://www.example.com"
            ],
            "x-arn-region": [
              "us-east-2"
            ]
          }
        }
      }
    },
    {
      "documentation": "test where explicit error is set",
      "params": {
        "CustomError": "This is an error!"
      },
      "expect": {
        "error": "This is an error!"
      }
    },
    {
      "documentation": "test where an ARN field is used in the error directly",
      "params": {
        "Arn": "arn:This is an error!:s3:us-east-2:012345678:outpost:op-1234"
      },
      "expect": {
        "error": "This is an error!"
      }
    },
    {
      "documentation": "test case where no fields are set",
      "params": {},
      "expect": {
        "error": "No fields were set"
      }
    }
  ]
)
@clientContextParams(
  Uri: {type: "string", documentation: "docs"}
  Arn: {type: "string", documentation: "docs"}
  CustomError: {type: "string", documentation: "docs"}
)
service FizzBuzz {}
