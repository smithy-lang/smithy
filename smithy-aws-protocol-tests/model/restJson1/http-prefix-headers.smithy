// This file defines test cases that test httpPrefix headers.
// See: https://awslabs.github.io/smithy/1.0/spec/http.html#httpprefixheaders-trait

$version: "1.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests
use aws.protocoltests.shared#StringMap

/// This examples adds headers to the input of a request and response by prefix.
@readonly
@http(uri: "/HttpPrefixHeaders", method: "GET")
@externalDocumentation("httpPrefixHeaders Trait": "https://awslabs.github.io/smithy/1.0/spec/http.html#httpprefixheaders-trait")
operation HttpPrefixHeaders  {
    input: HttpPrefixHeadersInputOutput,
    output: HttpPrefixHeadersInputOutput
}

apply HttpPrefixHeaders @httpRequestTests([
    {
        id: "RestJsonHttpPrefixHeadersArePresent",
        documentation: "Adds headers by prefix",
        protocol: restJson1,
        method: "GET",
        uri: "/HttpPrefixHeaders",
        body: "",
        headers: {
            "X-Foo": "Foo",
            "X-Foo-Abc": "Abc value",
            "X-Foo-Def": "Def value",
        },
        params: {
            foo: "Foo",
            fooMap: {
                Abc: "Abc value",
                Def: "Def value",
            }
        }
    },
    {
        id: "RestJsonHttpPrefixHeadersAreNotPresent",
        documentation: "No prefix headers are serialized because the value is empty",
        protocol: restJson1,
        method: "GET",
        uri: "/HttpPrefixHeaders",
        body: "",
        headers: {
            "X-Foo": "Foo"
        },
        params: {
            foo: "Foo",
            fooMap: {}
        }
    },
])

apply HttpPrefixHeaders @httpResponseTests([
    {
        id: "RestJsonHttpPrefixHeadersArePresent",
        documentation: "Adds headers by prefix",
        protocol: restJson1,
        code: 200,
        headers: {
            "X-Foo": "Foo",
            "X-Foo-Abc": "Abc value",
            "X-Foo-Def": "Def value",
        },
        params: {
            foo: "Foo",
            fooMap: {
                Abc: "Abc value",
                Def: "Def value",
            }
        }
    },
])

structure HttpPrefixHeadersInputOutput {
    @httpHeader("X-Foo")
    foo: String,

    @httpPrefixHeaders("X-Foo-")
    fooMap: StringMap,
}

/// Clients that perform this test extract all headers from the response.
@readonly
@http(uri: "/HttpPrefixHeadersResponse", method: "GET")
operation HttpPrefixHeadersResponse  {
    output: HttpPrefixHeadersResponseOutput
}

apply HttpPrefixHeadersResponse @httpResponseTests([
    {
        id: "HttpPrefixHeadersResponse",
        documentation: "(de)serializes all response headers",
        protocol: restJson1,
        code: 200,
        headers: {
            "X-Foo": "Foo",
            "Hello": "Hello"
        },
        params: {
            prefixHeaders: {
                "X-Foo": "Foo",
                "Hello": "Hello"
            }
        }
    },
])

structure HttpPrefixHeadersResponseOutput {
    @httpPrefixHeaders("")
    prefixHeaders: StringMap,
}
