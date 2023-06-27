$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet

@endpointRuleSet({
  "parameters": {
    "Region": {
      "type": "string",
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
        "properties": {}
      },
      "type": "endpoint"
    }
  ],
  "version": "1.3"
})
@clientContextParams(
  Region: {type: "string", documentation: "docs"}
)
service FizzBuzz {}
