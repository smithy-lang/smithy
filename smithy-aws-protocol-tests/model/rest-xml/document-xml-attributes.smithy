// This file defines test cases that serialize XML attributes.

$version: "0.5.0"

namespace aws.protocols.tests.restxml

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This example serializes an XML attributes on synthesized document.
@idempotent
@http(uri: "/XmlAttributes", method: "PUT")
operation XmlAttributes {
    input: XmlAttributesInputOutput,
    output: XmlAttributesInputOutput
}

apply XmlAttributes @httpRequestTests([
    {
        id: "XmlAttributes",
        documentation: "Serializes XML attributes on the synthesized document",
        protocol: "aws.rest-xml",
        method: "PUT",
        uri: "/XmlAttributes",
        body: """
              <XmlAttributesInputOutput test="test">
                  <foo>hi</foo>
              </XmlAttributesInputOutput>
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

apply XmlAttributes @httpResponseTests([
    {
        id: "XmlAttributes",
        documentation: "Serializes simple scalar properties",
        protocol: "aws.rest-xml",
        code: 200,
        body: """
              <XmlAttributesInputOutput test="test">
                  <foo>hi</foo>
              </XmlAttributesInputOutput>
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
    input: XmlAttributesOnPayloadInputOutput,
    output: XmlAttributesOnPayloadInputOutput
}

apply XmlAttributesOnPayload @httpRequestTests([
    {
        id: "XmlAttributesOnPayload",
        documentation: "Serializes XML attributes on the synthesized document",
        protocol: "aws.rest-xml",
        method: "PUT",
        uri: "/XmlAttributesOnPayload",
        body: """
              <XmlAttributesInputOutput test="test">
                  <foo>hi</foo>
              </XmlAttributesInputOutput>
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
        protocol: "aws.rest-xml",
        code: 200,
        body: """
              <XmlAttributesInputOutput test="test">
                  <foo>hi</foo>
              </XmlAttributesInputOutput>
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

structure XmlAttributesOnPayloadInputOutput {
    @httpPayload
    payload: XmlAttributesInputOutput
}
