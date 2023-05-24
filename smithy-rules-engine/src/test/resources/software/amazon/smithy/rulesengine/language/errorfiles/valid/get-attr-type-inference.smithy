$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet

@endpointRuleSet({
  "version": "1.3",
  "parameters": {
    "Bucket": {
      "type": "string",
      "documentation": "docs"
    }
  },
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
        },
        {
          "fn": "aws.parseArn",
          "argv": [
            {
              "ref": "Bucket"
            }
          ],
          "assign": "bucketArn"
        },
        {
          "fn": "getAttr",
          "argv": [
            {
              "ref": "bucketArn"
            },
            "resourceId[2]"
          ],
          "assign": "outpostId"
        }
      ],
      "endpoint": {
        "url": "https://{bucketArn#accountId}.{outpostId}.{bucketArn#region}"
      },
      "type": "endpoint"
    }
  ]
})
@clientContextParams(
  Bucket: {type: "string", documentation: "docs"}
)
service FizzBuzz {}
