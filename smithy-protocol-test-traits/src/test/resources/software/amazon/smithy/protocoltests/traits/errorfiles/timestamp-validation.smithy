namespace smithy.example

use smithy.test#httpRequestTests

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "foo3",
        protocol: "example",
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
