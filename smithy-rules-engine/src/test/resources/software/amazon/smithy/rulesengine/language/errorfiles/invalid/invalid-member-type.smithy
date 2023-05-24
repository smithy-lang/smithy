$version: "1.0"

namespace example

use smithy.rules#contextParam
use smithy.rules#endpointRuleSet

@endpointRuleSet({
 "version": "1.3",
 "parameters": {},
 "rules": [
  {
   "conditions": [],
   "documentation": "base rule",
   "endpoint": {
    "url": "https://endpoint.amazonaws.com",
    "headers": {}
   },
   "type": "endpoint"
  }
 ]
})
service FizzBuzz {
 operations: [GetResource]
}

operation GetResource {
 input: GetResourceInput
}

structure GetResourceInput {
 @contextParam(name: "ParameterBar")
 ResourceId: IntResourceId
}

integer IntResourceId
