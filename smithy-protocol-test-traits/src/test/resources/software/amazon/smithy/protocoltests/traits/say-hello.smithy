$version: "2.0"

namespace smithy.example

use smithy.test#httpRequestTests

@trait
@protocolDefinition
structure exampleProtocol {}

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "say_hello",
        protocol: exampleProtocol,
        params: {
            "greeting": "Hi",
            "name": "Teddy"
        },
        method: "POST",
        uri: "/",
        headers: {
            "X-Greeting": "Hi"
        },
        body: "{\"name\": \"Teddy\"}",
        bodyMediaType: "application/json"
    }
])
operation SayHello {
    input: SayHelloInput
}

structure SayHelloInput {
    @httpHeader("X-Greeting")
    greeting: String,

    name: String
}
