$version: "1.0"

namespace example

use smithy.rules#staticContextParams
use smithy.rules#contextParam

service FizzBuzz {
 operations: [GetResource, GetAnotherResource]
}

@staticContextParams(
  "ParameterFoo": {value: true},
  "ParamNotInRuleset": {value: "someValue"},
  "InconsistentParamType": {value: true}
)
operation GetResource {
 input: GetResourceInput
}

structure GetResourceInput {
 @contextParam(name: "ParameterBar")
 ResourceId: ResourceId
}

@staticContextParams(
    "ParameterFoo": {value: false},
    "ParamNotInRuleset": {value: "someOtherValue"},
    "InconsistentParamType": {value: "someValue"}
)
operation GetAnotherResource {
    input: GetAnotherResourceInput
}

structure GetAnotherResourceInput {
    @contextParam(name: "AnotherParameterBar")
    ResourceId: ResourceId
}

string ResourceId
integer IntResourceId
