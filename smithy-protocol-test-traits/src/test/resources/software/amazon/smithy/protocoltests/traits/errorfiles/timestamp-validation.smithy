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
    input: HasTimeInput
}

structure HasTimeInput {
    time: Timestamp,
}
