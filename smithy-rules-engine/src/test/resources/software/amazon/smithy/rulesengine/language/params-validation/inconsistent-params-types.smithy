$version: "1.0"

namespace example

use smithy.rules#staticContextParams

service FizzBuzz {
 operations: [GetResource, GetAnotherResource]
}

@staticContextParams(
  "InconsistentParamType": {value: true}
)
operation GetResource {
 input: GetResourceInput
}

structure GetResourceInput {
 ResourceId: ResourceId
}

@staticContextParams(
    "InconsistentParamType": {value: "some-string"}
)
operation GetAnotherResource {
    input: GetAnotherResourceInput
}

structure GetAnotherResourceInput {
    ResourceId: ResourceId
}

string ResourceId
