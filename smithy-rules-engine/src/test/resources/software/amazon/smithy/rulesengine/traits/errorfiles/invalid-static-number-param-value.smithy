$version: "1.0"

namespace smithy.example

use smithy.rules#staticContextParams

@staticContextParams(numberParam: {value: 42})
operation OperationNumber {}
