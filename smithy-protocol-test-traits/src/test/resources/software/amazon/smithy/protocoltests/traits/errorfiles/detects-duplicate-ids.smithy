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
    input: SayGoodbyeInput,
    output: SayGoodbyeOutput
}

@input
structure SayGoodbyeInput {}

@output
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
    input: SayHelloInput,
    output: SayHelloOutput
}

@input
structure SayHelloInput {}

@output
structure SayHelloOutput {}

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
    input: SayHello2Input,
    output: SayHello2Output
}

@input
structure SayHello2Input {}

@output
structure SayHello2Output {}
