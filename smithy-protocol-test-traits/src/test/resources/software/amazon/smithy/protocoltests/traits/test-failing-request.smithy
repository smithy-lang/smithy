namespace smithy.example

use smithy.test#failingHttpRequestTests

@trait
@protocolDefinition
structure exampleProtocol {}

@http(method: "POST", uri: "/")
@failingHttpRequestTests([
    {
        id: "say_hello",
        protocol: exampleProtocol,
        params: {
            "greeting": "Hi 😹",
            "name": "Teddy"
        },
        failureCause: { generic: "Greeting must only contain valid ascii" },
        appliesTo: "client"
    },
    {
        id: "say_hello",
        protocol: exampleProtocol,
        uri: "/?k=v",
        method: "POST",
        failureCause: { generic: "Invalid request body" },
        appliesTo: "server"
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
