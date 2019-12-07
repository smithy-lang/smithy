namespace smithy.example

use smithy.test#httpRequestTests

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "say_hello",
        protocol: "example",
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
operation SayHello(SayHelloInput)

structure SayHelloInput {
    @httpHeader("X-Greeting")
    greeting: String,

    name: String
}
