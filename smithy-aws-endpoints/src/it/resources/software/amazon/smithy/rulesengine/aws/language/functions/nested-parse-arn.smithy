$version: "2.0"

namespace example

use smithy.rules#endpointRuleSet

// Uses the closure-only AWS function aws.parseArn nested as an argument to getAttr. This guards
// that nested function arguments resolve against the classloader supplied to the assembler, not
// just the top-level condition function. The parsed ARN is bound with `assign` and proven set
// before indexing into it, so the model is type-valid when the function resolves.
@endpointRuleSet({
  "version": "1.3"
  "parameters": {
    "Arn": {
      "type": "string"
      "required": true
      "documentation": "an ARN"
    }
  }
  "rules": [
    {
      "documentation": "parse the ARN"
      "conditions": [
        {
          "fn": "aws.parseArn"
          "argv": [{ "ref": "Arn" }]
          "assign": "parsedArn"
        }
      ]
      "rules": [
        {
          "documentation": "extract the region from the parsed ARN (getAttr over a nested parse)"
          "conditions": [
            {
              "fn": "getAttr"
              "argv": [{ "ref": "parsedArn" }, "region"]
              "assign": "arnRegion"
            }
          ]
          "endpoint": { "url": "https://{arnRegion}.example.com" }
          "type": "endpoint"
        }
      ]
      "type": "tree"
    }
  ]
})
@smithy.rules#clientContextParams(
  Arn: { type: "string", documentation: "an ARN passed as a client context param" }
)
service NestedFn {}
