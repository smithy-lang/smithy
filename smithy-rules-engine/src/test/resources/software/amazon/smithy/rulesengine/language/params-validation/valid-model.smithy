$version: "1.0"

namespace example

use smithy.rules#staticContextParams
use smithy.rules#contextParam

service FizzBuzz {
 operations: [GetResource, GetAnotherResource]
}

@staticContextParams(
  "ParameterFoo": {value: "foo"},
  "ExtraParameter": {value: "someValue"}
)
operation GetResource {
 input: GetResourceInput
}

structure GetResourceInput {
 @contextParam(name: "ParameterBar")
 ResourceId: ResourceId
}

@staticContextParams(
    "ParameterFoo": {value: "bar"}
)
operation GetAnotherResource {
    input: GetAnotherResourceInput
}

structure GetAnotherResourceInput {
    @contextParam(name: "ParameterBar")
    ResourceId: ResourceId
}

string ResourceId
