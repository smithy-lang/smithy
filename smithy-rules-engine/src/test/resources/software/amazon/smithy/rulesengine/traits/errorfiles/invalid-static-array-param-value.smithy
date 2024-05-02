$version: "1.0"

namespace smithy.example

use smithy.rules#staticContextParams

@staticContextParams(arrayParam: {value: ["foo", 3]})
operation OperationArray {}
