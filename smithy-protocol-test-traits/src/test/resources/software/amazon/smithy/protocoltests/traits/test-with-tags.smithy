$version: "2.0"

namespace smithy.example

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@trait
@protocolDefinition
structure exampleProtocol {}

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "say_hello",
        protocol: exampleProtocol,
        params: {},
        method: "POST",
        uri: "/",
        tags: ["foo", "bar"],
    }
])
@httpResponseTests([
    {
        id: "say_goodbye",
        protocol: exampleProtocol,
        params: {},
        code: 200,
        tags: ["baz", "qux"],
    }
])
operation SaySomething {
    input: SayHelloInput,
    output: SayGoodbyeOutput
}

structure SayHelloInput {}
structure SayGoodbyeOutput {}
