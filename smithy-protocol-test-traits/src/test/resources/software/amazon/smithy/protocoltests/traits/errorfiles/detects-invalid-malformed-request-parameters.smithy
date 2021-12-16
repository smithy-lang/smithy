$version: "2.0"

namespace smithy.example

use smithy.test#httpMalformedRequestTests

@trait
@protocolDefinition
structure testProtocol {}

@http(method: "POST", uri: "/")
@httpMalformedRequestTests([
    {
        id: "definesMismatchedTestParameters",
        documentation: "Testing...",
        protocol: testProtocol,
        request: {
            body: "$foo:L",
            headers: {"X-Foo": "baz"},
            host: "example.com",
            method: "POST",
            uri: "/",
            queryParams: ["foo=baz"]
        },
        response: {
            code: 400,
            headers: {"X-Foo": "baz"},
            body: {
                assertion: {
                    messageRegex: "Invalid JSON: $bar:L"
                },
                mediaType: "application/json"
            }
        },
        tags: ["foo", "bar"],
        testParameters: {
            foo: ["a", "b", "c"],
            bar: ["d", "e"]
        }
    }
])
operation SayHello {
    input: SayHelloInput,
    output: SayHelloOutput
}

@input
structure SayHelloInput {
    @httpPayload
    body: String,

    @httpHeader("X-OmitMe")
    header: String,
}

@output
structure SayHelloOutput {}
