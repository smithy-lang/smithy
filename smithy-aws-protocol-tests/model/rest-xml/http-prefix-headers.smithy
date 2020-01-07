// This file defines test cases that test httpPrefix headers.
// See: https://awslabs.github.io/smithy/spec/http.html#httpprefixheaders-trait

$version: "0.5.0"

namespace aws.protocols.tests.restxml

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This examples adds headers to the input of a request and response by prefix.
@readonly
@http(uri: "/HttpPrefixHeaders", method: "GET")
@externalDocumentation("https://awslabs.github.io/smithy/spec/http.html#httpprefixheaders-trait")
operation HttpPrefixHeaders(HttpPrefixHeadersInputOutput) -> HttpPrefixHeadersInputOutput

apply HttpPrefixHeaders @httpRequestTests([
    {
        id: "HttpPrefixHeadersArePresent",
        description: "Adds headers by prefix",
        protocol: "aws.rest-xml",
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
        id: "HttpPrefixHeadersAreNotPresent",
        description: "No prefix headers are serialized because the value is empty",
        protocol: "aws.rest-xml",
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
        id: "HttpPrefixHeadersArePresent",
        description: "Adds headers by prefix",
        protocol: "aws.rest-xml",
        code: 200,
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
        id: "HttpPrefixHeadersAreNotPresent",
        description: "No prefix headers are serialized because the value is empty",
        protocol: "aws.rest-xml",
        code: 200,
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

structure HttpPrefixHeadersInputOutput {
    @httpHeader("X-Foo")
    foo: String,

    @httpPrefixHeaders("X-Foo-")
    fooMap: FooPrefixHeaders,
}

map FooPrefixHeaders {
    key: String,
    value: String,
}
