// This file defines test cases that serialize XML attributes.

$version: "2.0"
$operationInputSuffix: "Request"
$operationOutputSuffix: "Response"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This example serializes an XML attributes on synthesized document.
@idempotent
@http(uri: "/XmlAttributes", method: "PUT")
operation XmlAttributes {
    input := with [XmlAttributesInputOutput] {}
    output := with [XmlAttributesInputOutput] {}
}

apply XmlAttributes @httpRequestTests([
    {
        id: "XmlAttributes",
        documentation: "Serializes XML attributes on the synthesized document",
        protocol: restXml,
        method: "PUT",
        uri: "/XmlAttributes",
        body: """
              <XmlAttributesRequest test="test">
                  <foo>hi</foo>
              </XmlAttributesRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            foo: "hi",
            attr: "test"
        }
    },
    {
        id: "XmlAttributesWithEscaping",
        documentation: "Serializes XML attributes with escaped characters on the synthesized document",
        protocol: restXml,
        method: "PUT",
        uri: "/XmlAttributes",
        body: """
              <XmlAttributesRequest test="&lt;test&amp;mock&gt;">
                  <foo>hi</foo>
              </XmlAttributesRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            foo: "hi",
            attr: "<test&mock>"
        }
    },
])

apply XmlAttributes @httpResponseTests([
    {
        id: "XmlAttributes",
        documentation: "Serializes simple scalar properties",
        protocol: restXml,
        code: 200,
        body: """
              <XmlAttributesResponse test="test">
                  <foo>hi</foo>
              </XmlAttributesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            foo: "hi",
            attr: "test"
        }
    }
])

@mixin
structure XmlAttributesInputOutput {
    foo: String,

    @xmlAttribute
    @xmlName("test")
    attr: String,
}

/// This example serializes an XML attributes on a document targeted by httpPayload.
@idempotent
@http(uri: "/XmlAttributesOnPayload", method: "PUT")
operation XmlAttributesOnPayload {
    input := {
        @httpPayload
        payload: XmlAttributesPayloadRequest
    }
    output := {
        @httpPayload
        payload: XmlAttributesPayloadResponse
    }
}

apply XmlAttributesOnPayload @httpRequestTests([
    {
        id: "XmlAttributesOnPayload",
        documentation: "Serializes XML attributes on the synthesized document",
        protocol: restXml,
        method: "PUT",
        uri: "/XmlAttributesOnPayload",
        body: """
              <XmlAttributesPayloadRequest test="test">
                  <foo>hi</foo>
              </XmlAttributesPayloadRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            payload: {
                foo: "hi",
                attr: "test"
            }
        }
    }
])

apply XmlAttributesOnPayload @httpResponseTests([
    {
        id: "XmlAttributesOnPayload",
        documentation: "Serializes simple scalar properties",
        protocol: restXml,
        code: 200,
        body: """
              <XmlAttributesPayloadResponse test="test">
                  <foo>hi</foo>
              </XmlAttributesPayloadResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            payload: {
                foo: "hi",
                attr: "test"
            }
        }
    }
])

structure XmlAttributesPayloadRequest with [XmlAttributesInputOutput] {}

structure XmlAttributesPayloadResponse with [XmlAttributesInputOutput] {}
