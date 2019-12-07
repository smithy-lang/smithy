namespace smithy.example

use smithy.test#httpResponseTests

@error("client")
@httpError(400)
@httpResponseTests([
    {
        id: "invalid_greeting",
        protocol: "example",
        params: {foo: "baz", message: "Hi"},
        code: 400,
        headers: {"X-Foo": "baz"},
        body: "{\"message\": \"Hi\"}",
        bodyMediaType: "application/json",
    }
])
structure InvalidGreeting {
    @httpHeader("X-Foo")
    foo: String,

    message: String,
}
