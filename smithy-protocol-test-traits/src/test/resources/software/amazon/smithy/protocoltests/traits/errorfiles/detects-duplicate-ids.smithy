namespace smithy.example

use smithy.test#httpResponseTests
use smithy.test#httpRequestTests

@http(method: "POST", uri: "/")
@httpResponseTests([
    {
        id: "foo", // conflict with self and MyError
        protocol: "example",
        code: 200,
    },
    {
        id: "foo", // conflict with self and MyError
        protocol: "example",
        code: 200,
    }
])
operation SayGoodbye() -> SayGoodbyeOutput
structure SayGoodbyeOutput {}

@httpResponseTests([
    {
        id: "foo", // conflict with self and SayGoodbye
        protocol: "example",
        code: 200,
    },
    {
        id: "baz", // no conflict
        protocol: "example",
        code: 200,
    },
])
@error("client")
structure MyError {}

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "foo", // conflict with self and SayHello2
        protocol: "example",
        method: "POST",
        uri: "/",
    },
    {
        id: "foo", // conflict with self and SayHello2
        protocol: "example",
        method: "POST",
        uri: "/",
    },
])
operation SayHello(SayHelloInput)
structure SayHelloInput {}

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "foo", // conflict
        protocol: "example",
        method: "POST",
        uri: "/",
    },
    {
        id: "baz", // no conflict
        protocol: "example",
        method: "POST",
        uri: "/",
    }
])
operation SayHello2(SayHelloInput2)
structure SayHelloInput2 {}
