$version: "2.0"

namespace smithy.example

use smithy.test#httpRequestTests

@trait
@protocolDefinition
structure testProtocol {}

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "foo3",
        protocol: testProtocol,
        method: "POST",
        uri: "/",
        params: {
            time: 946845296
        }
    }
])
operation HasTime {
    input: HasTimeInput,
    output: HasTimeOutput
}

@input
structure HasTimeInput {
    time: Timestamp,
}

@output
structure HasTimeOutput {}
