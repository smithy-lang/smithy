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
        <!DOCTYPE root [
            <!ENTITY hifi "hifi">
            <!ENTITY hifi1 "&hifi;&hifi;&hifi;">
            <!ENTITY hifi2 "&hifi1;&hifi1;&hifi1;">
            <!ENTITY hifi3 "&hifi2;&hifi2;&hifi2;">
        ]>
        <XmlNamespacesResponse xmlns="https://example.com/">
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
