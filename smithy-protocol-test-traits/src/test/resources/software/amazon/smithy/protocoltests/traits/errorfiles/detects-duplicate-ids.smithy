namespace smithy.example

use smithy.test#httpResponseTests
use smithy.test#httpRequestTests

@trait
@protocolDefinition
structure testProtocol {}

@http(method: "POST", uri: "/")
@httpResponseTests([
    {
        id: "foo", // conflict with self and MyError
        protocol: testProtocol,
        code: 200,
    },
    {
        id: "foo", // conflict with self and MyError
        protocol: testProtocol,
        code: 200,
    }
])
operation SayGoodbye {
    output: SayGoodbyeOutput
}
structure SayGoodbyeOutput {}

@httpResponseTests([
    {
        id: "foo", // conflict with self and SayGoodbye
        protocol: testProtocol,
        code: 200,
    },
    {
        id: "baz", // no conflict
        protocol: testProtocol,
        code: 200,
    },
])
@error("client")
structure MyError {}

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "foo", // conflict with self and SayHello2
        protocol: testProtocol,
        method: "POST",
        uri: "/",
    },
    {
        id: "foo", // conflict with self and SayHello2
        protocol: testProtocol,
        method: "POST",
        uri: "/",
    },
])
operation SayHello {
    input: SayHelloInput
}
structure SayHelloInput {}

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "foo", // conflict
        protocol: testProtocol,
        method: "POST",
        uri: "/",
    },
    {
        id: "baz", // no conflict
        protocol: testProtocol,
        method: "POST",
        uri: "/",
    }
])
operation SayHello2 {
    input: SayHelloInput2
}

structure SayHelloInput2 {}
