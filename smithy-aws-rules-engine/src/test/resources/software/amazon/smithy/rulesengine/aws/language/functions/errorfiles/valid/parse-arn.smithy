$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet
use smithy.rules#endpointTests

@endpointRuleSet({
  "version": "1.3",
  "parameters": {
    "Region": {
      "type": "string",
      "builtIn": "AWS::Region",
      "documentation": "docs"
    },
    "Bucket": {
      "type": "string",
      "documentation": "docs"
    },
    "TestCaseId": {
      "type": "string",
      "documentation": "docs"
    }
  },
  "rules": [
    {
      "documentation": "tests of invalid arns",
      "conditions": [
        {
          "fn": "isSet",
          "argv": [
            {
              "ref": "TestCaseId"
            }
          ]
        },
        {
          "fn": "isSet",
          "argv": [
            {
              "ref": "Bucket"
            }
          ]
        },
        {
          "fn": "stringEquals",
          "argv": [
            "{TestCaseId}",
            "invalid-arn"
          ]
        }
      ],
      "type": "tree",
      "rules": [
        {
          "conditions": [
            {
              "fn": "aws.parseArn",
              "argv": ["{Bucket}"]
            }
          ],
          "type": "error",
          "error": "A valid ARN was parsed but `{Bucket}` is not a valid ARN"
        },
        {
          "conditions": [],
          "type": "error",
          "error": "Test case passed: `{Bucket}` is not a valid ARN."
        }
      ]
    },
    {
      "documentation": "tests of valid arns",
      "conditions": [
        {
          "fn": "isSet",
          "argv": [
            {
              "ref": "TestCaseId"
            }
          ]
        },
        {
          "fn": "isSet",
          "argv": [
            {
              "ref": "Bucket"
            }
          ]
        },
        {
          "fn": "stringEquals",
          "argv": [
            "{TestCaseId}",
            "valid-arn"
          ]
        }
      ],
      "type": "tree",
      "rules": [
        {
          "conditions": [
            {
              "fn": "aws.parseArn",
              "argv": ["{Bucket}"],
              "assign": "arn"
            },
            {
              "fn": "getAttr",
              "argv": [{"ref": "arn"}, "resourceId[0]"],
              "assign": "resource"
            }
          ],
          "type": "error",
          "error": "Test case passed: A valid ARN was parsed: service: `{arn#service}`, partition: `{arn#partition}, region: `{arn#region}`, accountId: `{arn#accountId}`, resource: `{resource}`"
        },
        {
          "conditions": [],
          "type": "error",
          "error": "Test case failed: `{Bucket}` is a valid ARN but parseArn failed to parse it."
        }
      ]
    },
    {
      "documentation": "region is set",
      "conditions": [
        {
          "fn": "isSet",
          "argv": [
            {
              "ref": "Region"
            }
          ]
        },
        {
          "fn": "aws.partition",
          "argv": [
            "{Region}"
          ],
          "assign": "partitionResult"
        }
      ],
      "rules": [
        {
          "documentation": "bucket is set, handle bucket specific endpoints",
          "conditions": [
            {
              "fn": "isSet",
              "argv": [
                {
                  "ref": "Bucket"
                }
              ]
            }
          ],
          "rules": [
            {
              "documentation": "bucket is set and is an arn",
              "conditions": [
                {
                  "fn": "aws.parseArn",
                  "argv": [
                    {
                      "ref": "Bucket"
                    }
                  ],
                  "assign": "bucketArn"
                }
              ],
              "rules": [
                {
                  "conditions": [
                    {
                      "fn": "getAttr",
                      "argv": [
                        {
                          "ref": "bucketArn"
                        },
                        "resourceId[1]"
                      ],
                      "assign": "outpostId"
                    }
                  ],
                  "rules": [
                    {
                      "conditions": [
                        {
                          "fn": "stringEquals",
                          "argv": [
                            "{outpostId}",
                            ""
                          ]
                        }
                      ],
                      "error": "OutpostId was empty",
                      "type": "error"
                    },
                    {
                      "conditions": [],
                      "endpoint": {
                        "url": "https://{outpostId}-{bucketArn#accountId}.{bucketArn#region}.{partitionResult#dnsSuffix}"
                      },
                      "type": "endpoint"
                    }
                  ],
                  "type": "tree"
                },
                {
                  "conditions": [],
                  "error": "Invalid ARN: outpostId was not set",
                  "type": "error"
                }
              ],
              "type": "tree"
            },
            {
              "documentation": "bucket can be used as a host label",
              "conditions": [
                {
                  "fn": "isValidHostLabel",
                  "argv": [
                    "{Bucket}",
                    false
                  ]
                }
              ],
              "endpoint": {
                "url": "https://{Bucket}.{Region}.amazonaws.com"
              },
              "type": "endpoint"
            },
            {
              "conditions": [],
              "documentation": "fallback: use bucket in the path",
              "endpoint": {
                "url": "https://{Region}.amazonaws.com/{Bucket}"
              },
              "type": "endpoint"
            }
          ],
          "type": "tree"
        },
        {
          "documentation": "region is set, bucket is not",
          "conditions": [],
          "endpoint": {
            "url": "https://{Region}.{partitionResult#dnsSuffix}"
          },
          "type": "endpoint"
        }
      ],
      "type": "tree"
    },
    {
      "documentation": "fallback when region is unset",
      "conditions": [],
      "error": "Region must be set to resolve a valid endpoint",
      "type": "error"
    }
  ]
})
@endpointTests(
  "version": "1.0",
  "testCases": [
    {
      "documentation": "arn + region resolution",
      "params": {
        "Bucket": "arn:aws:s3:us-east-2:012345678:outpost:op-1234",
        "Region": "us-east-2"
      },
      "expect": {
        "endpoint": {
          "url": "https://op-1234-012345678.us-east-2.amazonaws.com"
        }
      }
    },
    {
      "documentation": "arn, unset outpost id",
      "params": {
        "Bucket": "arn:aws:s3:us-east-2:012345678:outpost",
        "Region": "us-east-2"
      },
      "expect": {
        "error": "Invalid ARN: outpostId was not set"
      }
    },
    {
      "documentation": "arn, empty outpost id (tests that empty strings are handled properly during matching)",
      "params": {
        "Bucket": "arn:aws:s3:us-east-2:012345678:outpost::",
        "Region": "us-east-2"
      },
      "expect": {
        "error": "OutpostId was empty"
      }
    },
    {
      "documentation": "arn, empty outpost id (tests that ARN parsing considers a trailing colon)",
      "params": {
        "Bucket": "arn:aws:s3:us-east-2:012345678:outpost:",
        "Region": "us-east-2"
      },
      "expect": {
        "error": "OutpostId was empty"
      }
    },
    {
      "documentation": "valid hostlabel + region resolution",
      "params": {
        "Bucket": "mybucket",
        "Region": "us-east-2"
      },
      "expect": {
        "endpoint": {
          "url": "https://mybucket.us-east-2.amazonaws.com"
        }
      }
    },
    {
      "documentation": "not a valid hostlabel + region resolution",
      "params": {
        "Bucket": "99_a",
        "Region": "us-east-2"
      },
      "expect": {
        "endpoint": {
          "url": "https://us-east-2.amazonaws.com/99_a"
        }
      }
    },
    {
      "documentation": "no bucket",
      "params": {
        "Region": "us-east-2"
      },
      "expect": {
        "endpoint": {
          "url": "https://us-east-2.amazonaws.com"
        }
      }
    },
    {
      "documentation": "a string that is not a 6-part ARN",
      "params": {
        "TestCaseId": "invalid-arn",
        "Bucket": "asdf"
      },
      "expect": {
        "error": "Test case passed: `asdf` is not a valid ARN."
      }
    },
    {
      "documentation": "resource id MUST not be null",
      "params": {
        "TestCaseId": "invalid-arn",
        "Bucket": "arn:aws:s3:us-west-2:123456789012:"
      },
      "expect": {
        "error": "Test case passed: `arn:aws:s3:us-west-2:123456789012:` is not a valid ARN."
      }
    },
    {
      "documentation": "service MUST not be null",
      "params": {
        "TestCaseId": "invalid-arn",
        "Bucket": "arn:aws::us-west-2:123456789012:resource-id"
      },
      "expect": {
        "error": "Test case passed: `arn:aws::us-west-2:123456789012:resource-id` is not a valid ARN."
      }
    },
    {
      "documentation": "partition MUST not be null",
      "params": {
        "TestCaseId": "invalid-arn",
        "Bucket": "arn::s3:us-west-2:123456789012:resource-id"
      },
      "expect": {
        "error": "Test case passed: `arn::s3:us-west-2:123456789012:resource-id` is not a valid ARN."
      }
    },
    {
      "documentation": "region MAY be null",
      "params": {
        "TestCaseId": "valid-arn",
        "Bucket": "arn:aws:s3::123456789012:resource-id"
      },
      "expect": {
        "error": "Test case passed: A valid ARN was parsed: service: `s3`, partition: `aws, region: ``, accountId: `123456789012`, resource: `resource-id`"
      }
    },
    {
      "documentation": "accountId MAY be null",
      "params": {
        "TestCaseId": "valid-arn",
        "Bucket": "arn:aws:s3:us-east-1::resource-id"
      },
      "expect": {
        "error": "Test case passed: A valid ARN was parsed: service: `s3`, partition: `aws, region: `us-east-1`, accountId: ``, resource: `resource-id`"
      }
    },
    {
      "documentation": "accountId MAY be non-numeric",
      "params": {
        "TestCaseId": "valid-arn",
        "Bucket": "arn:aws:s3:us-east-1:abcd:resource-id"
      },
      "expect": {
        "error": "Test case passed: A valid ARN was parsed: service: `s3`, partition: `aws, region: `us-east-1`, accountId: `abcd`, resource: `resource-id`"
      }
    },
    {
      "documentation": "resource may contain both `:` and `/`",
      "params": {
        "TestCaseId": "valid-arn",
        "Bucket": "arn:aws:s3:us-east-1:123456789012:resource-id/resource-1:resource-2"
      },
      "expect": {
        "error": "Test case passed: A valid ARN was parsed: service: `s3`, partition: `aws, region: `us-east-1`, accountId: `123456789012`, resource: `resource-id`"
      }
    }
  ]
)
@clientContextParams(
  Bucket: {type: "string", documentation: "docs"}
  TestCaseId: {type: "string", documentation: "docs"}
)
service FizzBuzz {}
