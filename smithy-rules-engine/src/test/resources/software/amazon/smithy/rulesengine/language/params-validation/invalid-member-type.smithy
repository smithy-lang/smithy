$version: "1.0"

namespace example

use smithy.rules#contextParam

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
