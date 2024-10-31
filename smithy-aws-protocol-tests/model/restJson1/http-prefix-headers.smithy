// This file defines test cases that test httpPrefix headers.
// See: https://smithy.io/2.0/spec/http-bindings.html#httpprefixheaders-trait

$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests
use aws.protocoltests.shared#StringMap

/// This examples adds headers to the input of a request and response by prefix.
@readonly
@http(uri: "/HttpPrefixHeaders", method: "GET")
@externalDocumentation("httpPrefixHeaders Trait": "https://smithy.io/2.0/spec/http-bindings.html#httpprefixheaders-trait")
operation HttpPrefixHeaders  {
    input: HttpPrefixHeadersInput,
    output: HttpPrefixHeadersOutput
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
            "x-foo": "Foo",
            "x-foo-abc": "Abc value",
            "x-foo-def": "Def value",
        },
        params: {
            foo: "Foo",
            fooMap: {
                abc: "Abc value",
                def: "Def value",
            }
        }
    },
    {
        id: "RestJsonHttpPrefixHeadersAreNotPresent",
        documentation: "No prefix headers are serialized because the value is not present",
        protocol: restJson1,
        method: "GET",
        uri: "/HttpPrefixHeaders",
        body: "",
        headers: {
            "x-foo": "Foo"
        },
        params: {
            foo: "Foo",
            fooMap: {}
        },
        appliesTo: "client"
    },
    {
        id: "RestJsonHttpPrefixEmptyHeaders",
        documentation: "Serialize prefix headers were the value is present but empty"
        protocol: restJson1,
        method: "GET",
        uri: "/HttpPrefixHeaders",
        body: "",
        params: {
            fooMap: {
                abc: ""
            }
        },
        headers: {
            "x-foo-abc": ""
        }
        appliesTo: "client",
    },
])

apply HttpPrefixHeaders @httpResponseTests([
    {
        id: "RestJsonHttpPrefixHeadersArePresent",
        documentation: "Adds headers by prefix",
        protocol: restJson1,
        code: 200,
        headers: {
            "x-foo": "Foo",
            "x-foo-abc": "Abc value",
            "x-foo-def": "Def value",
        },
        params: {
            foo: "Foo",
            fooMap: {
                abc: "Abc value",
                def: "Def value",
            }
        }
    },
])

@input
structure HttpPrefixHeadersInput {
    @httpHeader("x-foo")
    foo: String,

    @httpPrefixHeaders("x-foo-")
    fooMap: StringMap,
}

@output
structure HttpPrefixHeadersOutput {
    @httpHeader("x-foo")
    foo: String,

    @httpPrefixHeaders("x-foo-")
    fooMap: StringMap,
}

/// Clients that perform this test extract all headers from the response.
@readonly
@http(uri: "/HttpPrefixHeadersResponse", method: "GET")
operation HttpPrefixHeadersInResponse  {
    input: HttpPrefixHeadersInResponseInput,
    output: HttpPrefixHeadersInResponseOutput
}

apply HttpPrefixHeadersInResponse @httpResponseTests([
    {
        id: "HttpPrefixHeadersResponse",
        documentation: "(de)serializes all response headers",
        protocol: restJson1,
        code: 200,
        headers: {
            "x-foo": "Foo",
            "hello": "Hello"
        },
        params: {
            prefixHeaders: {
                "x-foo": "Foo",
                "hello": "Hello"
            }
        }
    },
])

@input
structure HttpPrefixHeadersInResponseInput {}

@output
structure HttpPrefixHeadersInResponseOutput {
    @httpPrefixHeaders("")
    prefixHeaders: StringMap,
}
