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
        id: "say_hello_all",
        protocol: exampleProtocol,
        params: {},
        method: "POST",
        uri: "/"
    },
    {
        id: "say_hello_client",
        protocol: exampleProtocol,
        params: {},
        method: "POST",
        uri: "/",
        appliesTo: "client"
    },
    {
        id: "say_hello_server",
        protocol: exampleProtocol,
        params: {},
        method: "POST",
        uri: "/",
        appliesTo: "server"
    }
])
@httpResponseTests([
    {
        id: "say_goodbye_all",
        protocol: exampleProtocol,
        params: {},
        code: 200
    },
    {
        id: "say_goodbye_client",
        protocol: exampleProtocol,
        params: {},
        code: 200,
        appliesTo: "client"
    },
    {
        id: "say_goodbye_server",
        protocol: exampleProtocol,
        params: {},
        code: 200,
        appliesTo: "server"
    }
])
operation SaySomething {
    input: SayHelloInput,
    output: SayGoodbyeOutput
}

structure SayHelloInput {}
structure SayGoodbyeOutput {}
