$version: "2.0"

namespace smithy.example

use smithy.test#httpResponseTests
use smithy.test#httpRequestTests

@trait
@protocolDefinition
structure testProtocol {}

@http(method: "POST", uri: "/")
@httpResponseTests([
    {
        id: "foo1",
        protocol: testProtocol,
        code: 200,
        params: {
            invalid: true
        }
    }
])
operation SayGoodbye {
    input: SayGoodbyeInput,
    output: SayGoodbyeOutput
}

@input
structure SayGoodbyeInput {}

@output
structure SayGoodbyeOutput {}

@httpResponseTests([
    {
        id: "foo2",
        protocol: testProtocol,
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
        protocol: testProtocol,
        method: "POST",
        uri: "/",
        params: {
            badType: "hi"
        }
    }
])
operation SayHello {
    input: SayHelloInput,
    output: SayHelloOutput
}

@input
structure SayHelloInput {
    badType: Boolean
}

@output
structure SayHelloOutput {}
