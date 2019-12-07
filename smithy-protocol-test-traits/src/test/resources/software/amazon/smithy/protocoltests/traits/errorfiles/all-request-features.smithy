namespace smithy.example

use smithy.test#httpRequestTests

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "foo",
        documentation: "Testing...",
        protocol: "example",
        authScheme: "test",
        method: "POST",
        uri: "/",
        queryParams: {"foo": "baz"},
        forbidQueryParams: ["Nope"],
        requireQueryParams: ["Yap"],
        headers: {"X-Foo": "baz"},
        forbidHeaders: ["X-Nope"],
        requireHeaders: ["X-Yap"],
        body: "Hi",
        bodyMediaType: "text/plain",
        params: {body: "Hi"},
        vendorParams: {foo: "Bar"}
    }
])
operation SayHello(SayHelloInput)
structure SayHelloInput {
    @httpPayload
    body: String,
}
