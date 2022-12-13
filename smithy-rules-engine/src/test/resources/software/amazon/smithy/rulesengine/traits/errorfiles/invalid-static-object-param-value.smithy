$version: "1.0"

namespace smithy.example

use smithy.rules#staticContextParams

@staticContextParams(objectParam: {value: {key: "value"}})
operation OperationObject {}
