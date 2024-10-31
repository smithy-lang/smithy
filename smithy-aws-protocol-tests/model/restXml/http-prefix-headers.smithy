// This file defines test cases that test httpPrefix headers.
// See: https://smithy.io/2.0/spec/http-bindings.html#httpprefixheaders-trait

$version: "2.0"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This examples adds headers to the input of a request and response by prefix.
@readonly
@http(uri: "/HttpPrefixHeaders", method: "GET")
@externalDocumentation("httpPrefixHeaders Trait": "https://smithy.io/2.0/spec/http-bindings.html#httpprefixheaders-trait")
operation HttpPrefixHeaders {
    input: HttpPrefixHeadersInputOutput,
    output: HttpPrefixHeadersInputOutput
}

apply HttpPrefixHeaders @httpRequestTests([
    {
        id: "HttpPrefixHeadersArePresent",
        documentation: "Adds headers by prefix",
        protocol: restXml,
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
        id: "HttpPrefixHeadersAreNotPresent",
        documentation: "No prefix headers are serialized because the value is not present",
        protocol: restXml,
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
        id: "HttpPrefixEmptyHeaders",
        documentation: "Serialize prefix headers were the value is present but empty"
        protocol: restXml,
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
        id: "HttpPrefixHeadersArePresent",
        documentation: "Adds headers by prefix",
        protocol: restXml,
        code: 200,
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
        id: "HttpPrefixHeadersAreNotPresent",
        documentation: "No prefix headers are serialized because the value is empty",
        protocol: restXml,
        code: 200,
        body: "",
        headers: {
            "x-foo": "Foo"
        },
        params: {
            foo: "Foo",
            fooMap: {}
        }
    },
])

structure HttpPrefixHeadersInputOutput {
    @httpHeader("x-foo")
    foo: String,

    @httpPrefixHeaders("x-foo-")
    fooMap: FooPrefixHeaders,
}

map FooPrefixHeaders {
    key: String,
    value: String,
}
