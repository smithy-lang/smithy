// This file defines test cases that test HTTP payload bindings.
// See: https://smithy.io/2.0/spec/http-bindings.html#httppayload-trait

$version: "2.0"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use aws.protocoltests.shared#TextPlainBlob
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This example serializes a blob shape in the payload.
///
/// In this example, no XML document is synthesized because the payload is
/// not a structure or a union type.
@http(uri: "/HttpPayloadTraits", method: "POST")
operation HttpPayloadTraits {
    input: HttpPayloadTraitsInputOutput,
    output: HttpPayloadTraitsInputOutput
}

apply HttpPayloadTraits @httpRequestTests([
    {
        id: "HttpPayloadTraitsWithBlob",
        documentation: "Serializes a blob in the HTTP payload",
        protocol: restXml,
        method: "POST",
        uri: "/HttpPayloadTraits",
        body: "blobby blob blob",
        headers: {
            "X-Foo": "Foo"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            foo: "Foo",
            blob: "blobby blob blob"
        }
    },
    {
        id: "HttpPayloadTraitsWithNoBlobBody",
        documentation: "Serializes an empty blob in the HTTP payload",
        protocol: restXml,
        method: "POST",
        uri: "/HttpPayloadTraits",
        body: "",
        headers: {
            "X-Foo": "Foo"
        },
        params: {
            foo: "Foo"
        }
    },
])

apply HttpPayloadTraits @httpResponseTests([
    {
        id: "HttpPayloadTraitsWithBlob",
        documentation: "Serializes a blob in the HTTP payload",
        protocol: restXml,
        code: 200,
        body: "blobby blob blob",
        headers: {
            "X-Foo": "Foo"
        },
        params: {
            foo: "Foo",
            blob: "blobby blob blob"
        }
    },
    {
        id: "HttpPayloadTraitsWithNoBlobBody",
        documentation: "Serializes an empty blob in the HTTP payload",
        protocol: restXml,
        code: 200,
        body: "",
        headers: {
            "X-Foo": "Foo"
        },
        params: {
            foo: "Foo"
        }
    }
])

structure HttpPayloadTraitsInputOutput {
    @httpHeader("X-Foo")
    foo: String,

    @httpPayload
    blob: Blob,
}

/// This example uses a `@mediaType` trait on the payload to force a custom
/// content-type to be serialized.
@http(uri: "/HttpPayloadTraitsWithMediaType", method: "POST")
operation HttpPayloadTraitsWithMediaType {
    input: HttpPayloadTraitsWithMediaTypeInputOutput,
    output: HttpPayloadTraitsWithMediaTypeInputOutput
}

apply HttpPayloadTraitsWithMediaType @httpRequestTests([
    {
        id: "HttpPayloadTraitsWithMediaTypeWithBlob",
        documentation: "Serializes a blob in the HTTP payload with a content-type",
        protocol: restXml,
        method: "POST",
        uri: "/HttpPayloadTraitsWithMediaType",
        body: "blobby blob blob",
        headers: {
            "X-Foo": "Foo",
            "Content-Type": "text/plain"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            foo: "Foo",
            blob: "blobby blob blob"
        }
    }
])

apply HttpPayloadTraitsWithMediaType @httpResponseTests([
    {
        id: "HttpPayloadTraitsWithMediaTypeWithBlob",
        documentation: "Serializes a blob in the HTTP payload with a content-type",
        protocol: restXml,
        code: 200,
        body: "blobby blob blob",
        headers: {
            "X-Foo": "Foo",
            "Content-Type": "text/plain"
        },
        params: {
            foo: "Foo",
            blob: "blobby blob blob"
        }
    }
])

structure HttpPayloadTraitsWithMediaTypeInputOutput {
    @httpHeader("X-Foo")
    foo: String,

    @httpPayload
    blob: TextPlainBlob,
}

/// This example serializes a structure in the payload.
///
/// Note that serializing a structure changes the wrapper element name
/// to match the targeted structure.
@idempotent
@http(uri: "/HttpPayloadWithStructure", method: "PUT")
operation HttpPayloadWithStructure {
    input: HttpPayloadWithStructureInputOutput,
    output: HttpPayloadWithStructureInputOutput
}

apply HttpPayloadWithStructure @httpRequestTests([
    {
        id: "HttpPayloadWithStructure",
        documentation: "Serializes a structure in the payload",
        protocol: restXml,
        method: "PUT",
        uri: "/HttpPayloadWithStructure",
        body: """
              <NestedPayload>
                  <greeting>hello</greeting>
                  <name>Phreddy</name>
              </NestedPayload>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            nested: {
                greeting: "hello",
                name: "Phreddy"
            }
        }
    }
])

apply HttpPayloadWithStructure @httpResponseTests([
    {
        id: "HttpPayloadWithStructure",
        documentation: "Serializes a structure in the payload",
        protocol: restXml,
        code: 200,
        body: """
            <NestedPayload>
                <greeting>hello</greeting>
                <name>Phreddy</name>
            </NestedPayload>
            """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            nested: {
                greeting: "hello",
                name: "Phreddy"
            }
        }
    }
])

structure HttpPayloadWithStructureInputOutput {
    @httpPayload
    nested: NestedPayload,
}

structure NestedPayload {
    greeting: String,
    name: String,
}

/// The following example serializes a payload that uses an XML name,
/// changing the wrapper name.
@idempotent
@http(uri: "/HttpPayloadWithXmlName", method: "PUT")
operation HttpPayloadWithXmlName {
    input: HttpPayloadWithXmlNameInputOutput,
    output: HttpPayloadWithXmlNameInputOutput
}

apply HttpPayloadWithXmlName @httpRequestTests([
    {
        id: "HttpPayloadWithXmlName",
        documentation: "Serializes a structure in the payload using a wrapper name based on xmlName",
        protocol: restXml,
        method: "PUT",
        uri: "/HttpPayloadWithXmlName",
        body: "<Hello><name>Phreddy</name></Hello>",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            nested: {
                name: "Phreddy"
            }
        }
    }
])

apply HttpPayloadWithXmlName @httpResponseTests([
    {
        id: "HttpPayloadWithXmlName",
        documentation: "Serializes a structure in the payload using a wrapper name based on xmlName",
        protocol: restXml,
        code: 200,
        body: "<Hello><name>Phreddy</name></Hello>",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            nested: {
                name: "Phreddy"
            }
        }
    }
])

structure HttpPayloadWithXmlNameInputOutput {
    @httpPayload
    nested: PayloadWithXmlName,
}

@xmlName("Hello")
structure PayloadWithXmlName {
    name: String
}

/// The following example serializes a body that uses an XML name,
/// changing the wrapper name.
@idempotent
@http(uri: "/BodyWithXmlName", method: "PUT")
operation BodyWithXmlName {
    input: BodyWithXmlNameInputOutput,
    output: BodyWithXmlNameInputOutput
}

apply BodyWithXmlName @httpRequestTests([
    {
        id: "BodyWithXmlName",
        documentation: "Serializes a payload using a wrapper name based on the xmlName",
        protocol: restXml,
        method: "PUT",
        uri: "/BodyWithXmlName",
        body: "<Ahoy><nested><name>Phreddy</name></nested></Ahoy>",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            nested: {
                name: "Phreddy"
            }
        }
    }
])

apply BodyWithXmlName @httpResponseTests([
    {
        id: "BodyWithXmlName",
        documentation: "Serializes a payload using a wrapper name based on the xmlName",
        protocol: restXml,
        code: 200,
        body: "<Ahoy><nested><name>Phreddy</name></nested></Ahoy>",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            nested: {
                name: "Phreddy"
            }
        }
    }
])

@xmlName("Ahoy")
structure BodyWithXmlNameInputOutput {
    nested: PayloadWithXmlName,
}

/// The following example serializes a payload that uses an XML name
/// on the member, changing the wrapper name.

@idempotent
@http(uri: "/HttpPayloadWithMemberXmlName", method: "PUT")
operation HttpPayloadWithMemberXmlName {
    input: HttpPayloadWithMemberXmlNameInputOutput,
    output: HttpPayloadWithMemberXmlNameInputOutput
}

apply HttpPayloadWithMemberXmlName @httpRequestTests([
    {
        id: "HttpPayloadWithMemberXmlName",
        documentation: "Serializes a structure in the payload using a wrapper name based on member xmlName",
        protocol: restXml,
        method: "PUT",
        uri: "/HttpPayloadWithMemberXmlName",
        body: "<Hola><name>Phreddy</name></Hola>",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            nested: {
                name: "Phreddy"
            }
        }
    }
])

apply HttpPayloadWithMemberXmlName @httpResponseTests([
    {
        id: "HttpPayloadWithMemberXmlName",
        documentation: "Serializes a structure in the payload using a wrapper name based on member xmlName",
        protocol: restXml,
        code: 200,
        body: "<Hola><name>Phreddy</name></Hola>",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            nested: {
                name: "Phreddy"
            }
        }
    }
])

structure HttpPayloadWithMemberXmlNameInputOutput {
    @httpPayload
    @xmlName("Hola")
    nested: PayloadWithXmlName,
}

/// The following example serializes a payload that uses an XML namespace.
@idempotent
@http(uri: "/HttpPayloadWithXmlNamespace", method: "PUT")
operation HttpPayloadWithXmlNamespace {
    input: HttpPayloadWithXmlNamespaceInputOutput,
    output: HttpPayloadWithXmlNamespaceInputOutput
}

apply HttpPayloadWithXmlNamespace @httpRequestTests([
    {
        id: "HttpPayloadWithXmlNamespace",
        documentation: "Serializes a structure in the payload using a wrapper with an XML namespace",
        protocol: restXml,
        method: "PUT",
        uri: "/HttpPayloadWithXmlNamespace",
        body: """
              <PayloadWithXmlNamespace xmlns="http://foo.com">
                  <name>Phreddy</name>
              </PayloadWithXmlNamespace>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            nested: {
                name: "Phreddy"
            }
        }
    }
])

apply HttpPayloadWithXmlNamespace @httpResponseTests([
    {
        id: "HttpPayloadWithXmlNamespace",
        documentation: "Serializes a structure in the payload using a wrapper with an XML namespace",
        protocol: restXml,
        code: 200,
        body: """
              <PayloadWithXmlNamespace xmlns="http://foo.com">
                  <name>Phreddy</name>
              </PayloadWithXmlNamespace>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            nested: {
                name: "Phreddy"
            }
        }
    }
])

structure HttpPayloadWithXmlNamespaceInputOutput {
    @httpPayload
    nested: PayloadWithXmlNamespace,
}

@xmlNamespace(uri: "http://foo.com")
structure PayloadWithXmlNamespace {
    name: String
}

/// The following example serializes a payload that uses an XML namespace.
@idempotent
@http(uri: "/HttpPayloadWithXmlNamespaceAndPrefix", method: "PUT")
operation HttpPayloadWithXmlNamespaceAndPrefix {
    input: HttpPayloadWithXmlNamespaceAndPrefixInputOutput,
    output: HttpPayloadWithXmlNamespaceAndPrefixInputOutput
}

apply HttpPayloadWithXmlNamespaceAndPrefix @httpRequestTests([
    {
        id: "HttpPayloadWithXmlNamespaceAndPrefix",
        documentation: "Serializes a structure in the payload using a wrapper with an XML namespace",
        protocol: restXml,
        method: "PUT",
        uri: "/HttpPayloadWithXmlNamespaceAndPrefix",
        body: """
              <PayloadWithXmlNamespaceAndPrefix xmlns:baz="http://foo.com">
                  <name>Phreddy</name>
              </PayloadWithXmlNamespaceAndPrefix>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            nested: {
                name: "Phreddy"
            }
        }
    }
])

apply HttpPayloadWithXmlNamespaceAndPrefix @httpResponseTests([
    {
        id: "HttpPayloadWithXmlNamespaceAndPrefix",
        documentation: "Serializes a structure in the payload using a wrapper with an XML namespace",
        protocol: restXml,
        code: 200,
        body: """
              <PayloadWithXmlNamespaceAndPrefix xmlns:baz="http://foo.com">
                  <name>Phreddy</name>
              </PayloadWithXmlNamespaceAndPrefix>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            nested: {
                name: "Phreddy"
            }
        }
    }
])

structure HttpPayloadWithXmlNamespaceAndPrefixInputOutput {
    @httpPayload
    nested: PayloadWithXmlNamespaceAndPrefix,
}

@xmlNamespace(uri: "http://foo.com", prefix: "baz")
structure PayloadWithXmlNamespaceAndPrefix {
    name: String
}


/// This example serializes a union in the payload.
@idempotent
@http(uri: "/HttpPayloadWithUnion", method: "PUT")
operation HttpPayloadWithUnion {
    input: HttpPayloadWithUnionInputOutput,
    output: HttpPayloadWithUnionInputOutput
}

apply HttpPayloadWithUnion @httpRequestTests([
    {
        id: "RestXmlHttpPayloadWithUnion",
        documentation: "Serializes a union in the payload.",
        protocol: restXml,
        method: "PUT",
        uri: "/HttpPayloadWithUnion",
        body: """
              <UnionPayload>
                  <greeting>hello</greeting>
              </UnionPayload>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            nested: {
                greeting: "hello"
            }
        }
    },
    {
        id: "RestXmlHttpPayloadWithUnsetUnion",
        documentation: "No payload is sent if the union has no value.",
        protocol: restXml,
        method: "PUT",
        uri: "/HttpPayloadWithUnion",
        body: "",
        headers: {
            "Content-Type": "application/xml",
        },
        params: {}
    }
])

apply HttpPayloadWithUnion @httpResponseTests([
    {
        id: "RestXmlHttpPayloadWithUnion",
        documentation: "Serializes a union in the payload.",
        protocol: restXml,
        code: 200,
        body: """
              <UnionPayload>
                  <greeting>hello</greeting>
              </UnionPayload>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
        },
        params: {
            nested: {
                greeting: "hello"
            }
        }
    },
    {
        id: "RestXmlHttpPayloadWithUnsetUnion",
        documentation: "No payload is sent if the union has no value.",
        protocol: restXml,
        code: 200,
        body: "",
        headers: {
            "Content-Length": "0"
        },
        params: {}
    }
])

structure HttpPayloadWithUnionInputOutput {
    @httpPayload
    nested: UnionPayload,
}

union UnionPayload {
    greeting: String
}
