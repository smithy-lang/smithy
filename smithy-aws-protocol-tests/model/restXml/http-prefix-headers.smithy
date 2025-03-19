// This file defines test cases that test httpPrefix headers.
// See: https://smithy.io/2.0/spec/http-bindings.html#httpprefixheaders-trait

$version: "2.0"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use aws.protocoltests.shared#StringMap
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

/// Clients that perform this test extract all headers from the response.
@readonly
@http(uri: "/HttpEmptyPrefixHeaders", method: "GET")
operation HttpEmptyPrefixHeaders  {
    input := {
        @httpPrefixHeaders("")
        prefixHeaders: StringMap

        @httpHeader("hello")
        specificHeader: String
    }
    output := {
        @httpPrefixHeaders("")
        prefixHeaders: StringMap

        @httpHeader("hello")
        specificHeader: String
    }
}

apply HttpEmptyPrefixHeaders @httpRequestTests([
    {
        id: "HttpEmptyPrefixHeadersRequestClient"
        documentation: "Serializes all request headers, using specific when present"
        protocol: restXml
        method: "GET"
        uri: "/HttpEmptyPrefixHeaders"
        body: ""
        headers: {
            "x-foo": "Foo",
            "hello": "There"
        }
        params: {
            prefixHeaders: {
                "x-foo": "Foo",
                "hello": "Hello"
            }
            specificHeader: "There"
        }
        appliesTo: "client"
    }
    {
        id: "HttpEmptyPrefixHeadersRequestServer"
        documentation: "Deserializes all request headers with the same for prefix and specific"
        protocol: restXml
        method: "GET"
        uri: "/HttpEmptyPrefixHeaders"
        body: ""
        headers: {
            "x-foo": "Foo",
            "hello": "There"
        }
        params: {
            prefixHeaders: {
                "x-foo": "Foo",
                "hello": "There"
            }
            specificHeader: "There"
        }
        appliesTo: "server"
    }
])

apply HttpEmptyPrefixHeaders @httpResponseTests([
    {
        id: "HttpEmptyPrefixHeadersResponseClient"
        documentation: "Deserializes all response headers with the same for prefix and specific"
        protocol: restXml
        code: 200
        headers: {
            "x-foo": "Foo",
            "hello": "There"
        }
        params: {
            prefixHeaders: {
                "x-foo": "Foo",
                "hello": "There"
            }
            specificHeader: "There"
        }
        appliesTo: "client"
    }
    {
        id: "HttpEmptyPrefixHeadersResponseServer"
        documentation: "Serializes all response headers, using specific when present"
        protocol: restXml
        code: 200
        headers: {
            "x-foo": "Foo",
            "hello": "There"
        }
        params: {
            prefixHeaders: {
                "x-foo": "Foo",
                "hello": "Hello"
            }
            specificHeader: "There"
        }
        appliesTo: "server"
    }
])
