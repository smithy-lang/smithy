$version: "1.0"

namespace smithy.example

use smithy.rules#staticContextParams

@staticContextParams(nullParam: {value: null})
operation OperationNull {}
