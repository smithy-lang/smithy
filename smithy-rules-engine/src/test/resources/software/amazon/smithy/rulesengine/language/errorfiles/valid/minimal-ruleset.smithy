$version: "2.0"

namespace example

use smithy.rules#endpointRuleSet

@endpointRuleSet({
  "version": "1.3",
  "parameters": {
    "Region": {
      "builtIn": "AWS::Region",
      "required": true,
      "type": "String",
      "documentation": "docs"
    }
  },
  "rules": [
    {
      "conditions": [],
      "documentation": "base rule",
      "endpoint": {
        "url": "https://{Region}.amazonaws.com",
        "properties": {
          "authSchemes": [
            {
              "name": "sigv4",
              "signingName": "serviceName",
              "signingRegion": "{Region}"
            }
          ]
        },
        "headers": {}
      },
      "type": "endpoint"
    }
  ]
})
service FizzBuzz {}
