$version: "1.0"

namespace example

use smithy.rules#staticContextParams
use smithy.rules#contextParam

service FizzBuzz {
    operations: [GetResource, GetAnotherResource]
}

@staticContextParams(
    "ParameterFoo": {value: true},
)
operation GetResource {
    input: GetResourceInput
}

structure GetResourceInput {
    @contextParam(name: "ParameterBar")
    ResourceId: ResourceId
}

operation GetAnotherResource {
    input: GetAnotherResourceInput
}

structure GetAnotherResourceInput {
    @contextParam(name: "ParameterBar")
    ResourceId: ResourceIdBool
}

string ResourceId
boolean ResourceIdBool
