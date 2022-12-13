$version: "1.0"

namespace smithy.example

use smithy.rules#staticContextParams

@staticContextParams(arrayParam: {value: ["foo", "bar"]})
operation OperationArray {}
