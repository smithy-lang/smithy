$version: "2.0"

namespace smithy.example

use smithy.test#httpMalformedRequestTests

@trait
@protocolDefinition
structure testProtocol {}

@http(method: "POST", uri: "/")
@httpMalformedRequestTests([
    {
        id: "bodyRegex",
        documentation: "Testing...",
        protocol: testProtocol,
        request: {
            body: "Hi",
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
                    messageRegex: "Invalid JSON: .*"
                },
                mediaType: "application/json"
            }
        },
        tags: ["foo", "bar"]
    },
    {
        id: "body",
        documentation: "Testing...",
        protocol: testProtocol,
        request: {
            body: "Hi",
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
                    contents: """
                    {
                        "message" : "Invalid JSON"
                    }"""
                },
                mediaType: "application/json"
            }
        },
        tags: ["foo", "bar"]
    },
    {
        id: "noResponseBodyAssertion",
        documentation: "Testing...",
        protocol: testProtocol,
        request: {
            body: "Hi",
            headers: {"X-Foo": "baz"},
            host: "example.com",
            method: "POST",
            uri: "/",
            queryParams: ["foo=baz"]
        },
        response: {
            code: 400,
            headers: {"X-Foo": "baz"}
        },
        tags: ["foo", "bar"]
    },
    {
        id: "parameterized",
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
            headers: {"X-Foo": "$bar:L"}
        },
        tags: ["foo", "bar"],
        testParameters: {
            "foo" : ["a", "b", "c"],
            "bar" : ["d", "e", "f"]
        }
    }
    {
        id: "noResponseBodyAssertionWithMediaType"
        documentation: "Testing..."
        protocol: testProtocol
        request: {
            body: "Zm9vCg=="
            bodyMediaType: "application/cbor"
            headers: {"X-Foo": "baz"}
            host: "example.com"
            method: "POST"
            uri: "/"
            queryParams: ["foo=baz"]
        },
        response: {
            code: 400
            headers: {"X-Foo": "baz"}
        },
        tags: ["foo", "bar"]
    },
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
