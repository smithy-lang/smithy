$version: "2.0"

namespace smithy.example

use smithy.test#httpRequestTests

@trait
@protocolDefinition
structure testProtocol {}

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "foo1",
        protocol: testProtocol,
        method: "POST",
        uri: "/",
        params: {
            type: true
        },
        bodyMediaType: "application/xml",
        body: """
        <XmlNamespacesResponse xmlns="http://foo.com" xmlns="https://example.com/">
            <nested>
                <foo xmlns:baz="http://baz.com">Foo</foo>
                <values xmlns="http://qux.com">
                    <member xmlns="http://bux.com">Bar</member>
                    <member xmlns="http://bux.com">Baz</member>
                </values>
            </nested>
            <RequestId>requestid</RequestId>
        </XmlNamespacesResponse>
        """
    }
])
operation SayHello {
    input: SayHelloInput,
    output: SayHelloOutput
}

@input
structure SayHelloInput {
    type: Boolean
}

@output
structure SayHelloOutput {}
