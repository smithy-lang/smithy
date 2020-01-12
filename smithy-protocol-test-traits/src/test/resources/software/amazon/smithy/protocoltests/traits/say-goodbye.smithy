namespace smithy.example

use smithy.test#httpResponseTests

@http(method: "POST", uri: "/")
@httpResponseTests([
    {
        id: "say_goodbye",
        protocol: "example",
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
