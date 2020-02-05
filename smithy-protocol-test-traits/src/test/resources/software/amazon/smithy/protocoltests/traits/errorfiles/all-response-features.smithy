namespace smithy.example

use smithy.test#httpResponseTests

@trait
@protocolDefinition
structure testProtocol {}

@trait
@authDefinition
structure testScheme {}

@readonly
@http(method: "GET", uri: "/")
@httpResponseTests([
    {
        id: "foo2",
        protocol: testProtocol,
        authScheme: testScheme,
        code: 200,
        headers: {
            "X-Blah": "Blarg"
        },
        forbidHeaders: ["X-Nope"],
        requireHeaders: ["X-Yep"],
        body: "Baz",
        bodyMediaType: "text/plain",
        params: {
            bar: "Baz",
            blah: "Blarg"
        },
        vendorParams: {
            lorem: "ipsum"
        },
        documentation: "Hi"
    }
])
operation GetFoo {
    output: GetFooOutput
}

structure GetFooOutput {
    @httpPayload
    bar: String,

    @httpHeader("X-Blah")
    blah: String,
}
