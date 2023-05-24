$version: "2.0"

namespace example

use smithy.rules#endpointRuleSet

@endpointRuleSet({
  "parameters": {
    "Region": {
      "type": "string",
      "builtIn": "AWS::Region",
      "required": false,
      "deprecated": {
        "message": "use blahdeblah region instead"
      },
      "documentation": "docs"
    }
  },
  "rules": [
    {
      "documentation": "base rule",
      "conditions": [
        {
          "fn": "isSet",
          "argv": [
            {
              "ref": "Region"
            }
          ]
        }
      ],
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
        }
      },
      "type": "endpoint"
    }
  ],
  "version": "1.3"
})
service FizzBuzz {}
