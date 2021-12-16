$version: "2.0"

namespace smithy.example

use smithy.test#httpResponseTests

@trait
@protocolDefinition
structure exampleProtocol {}

@http(method: "POST", uri: "/")
@httpResponseTests([
    {
        id: "say_goodbye",
        protocol: exampleProtocol,
        params: {farewell: "Bye"},
        code: 200,
        headers: {
            "X-Farewell": "Bye",
            "Content-Length": "0"
        }
    }
])
operation SayGoodbye {
    output: SayGoodbyeOutput
}

structure SayGoodbyeOutput {
    @httpHeader("X-Farewell")
    farewell: String,
}
