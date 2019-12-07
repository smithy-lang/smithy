namespace smithy.example

use smithy.test#httpResponseTests
use smithy.test#httpRequestTests

@http(method: "POST", uri: "/")
@httpResponseTests([
    {
        id: "foo1",
        protocol: "example",
        code: 200,
        params: {
            invalid: true
        }
    }
])
operation SayGoodbye() -> SayGoodbyeOutput
structure SayGoodbyeOutput {}

@httpResponseTests([
    {
        id: "foo2",
        protocol: "example",
        code: 200,
        params: {
            foo: "Hi"
        }
    }
])
@error("client")
structure MyError {
    foo: Integer,
}

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "foo3",
        protocol: "example",
        method: "POST",
        uri: "/",
        params: {
            badType: "hi"
        }
    }
])
operation SayHello(SayHelloInput)
structure SayHelloInput {
    badType: Boolean
}
