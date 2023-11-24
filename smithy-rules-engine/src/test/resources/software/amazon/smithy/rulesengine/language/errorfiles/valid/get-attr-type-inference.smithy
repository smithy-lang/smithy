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
          "fn": "parseURL",
          "argv": [
            "{Bucket}"
          ],
          "assign": "bucketUrl"
        },
        {
          "fn": "getAttr",
          "argv": [
            {
              "ref": "bucketUrl"
            },
            "path"
          ],
          "assign": "path"
        }
      ],
      "endpoint": {
        "url": "https://{bucketUrl#authority}/{path}"
      },
      "type": "endpoint"
    }
  ]
})
@clientContextParams(
  Bucket: {type: "string", documentation: "docs"}
)
service FizzBuzz {}
