// This file defines test cases that serialize inline documents.

$version: "1.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

// Define some shapes shared throughout these test cases.
document Document

/// This example serializes an inline document as part of the payload.
@idempotent
@http(uri: "/InlineDocument", method: "PUT")
operation InlineDocument {
    input: InlineDocumentInputOutput,
    output: InlineDocumentInputOutput
}

structure InlineDocumentInputOutput {
    stringValue: String,
    documentValue: Document,
}

apply InlineDocument @httpRequestTests([
    {
        id: "InlineDocumentInput",
        documentation: "Serializes inline documents as part of the JSON request payload with no escaping.",
        protocol: restJson1,
        method: "PUT",
        uri: "/InlineDocument",
        body: """
              {
                  "stringValue": "string",
                  "documentValue": {
                      "foo": "bar"
                  }
              }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            stringValue: "string",
            documentValue: {
                foo: "bar"
            }
        }
    }
])

apply InlineDocument @httpResponseTests([
    {
        id: "InlineDocumentOutput",
        documentation: "Serializes inline documents as part of the JSON response payload with no escaping.",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "stringValue": "string",
                "documentValue": {
                    "foo": "bar"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            stringValue: "string",
            documentValue: {
                foo: "bar"
            }
        }
    }
])

/// This example serializes an inline document as the entire HTTP payload.
@idempotent
@http(uri: "/InlineDocumentAsPayload", method: "PUT")
operation InlineDocumentAsPayload {
    input: InlineDocumentAsPayloadInputOutput,
    output: InlineDocumentAsPayloadInputOutput
}

structure InlineDocumentAsPayloadInputOutput {
    @httpPayload
    documentValue: Document,
}

apply InlineDocumentAsPayload @httpRequestTests([
    {
        id: "InlineDocumentAsPayloadInput",
        documentation: "Serializes an inline document as the target of the httpPayload trait.",
        protocol: restJson1,
        method: "PUT",
        uri: "/InlineDocumentAsPayload",
        body: """
              {
                  "foo": "bar"
              }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            documentValue: {
                foo: "bar"
            }
        }
    }
])

apply InlineDocumentAsPayload @httpResponseTests([
    {
        id: "InlineDocumentAsPayloadInputOutput",
        documentation: "Serializes an inline document as the target of the httpPayload trait.",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "foo": "bar"
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            documentValue: {
                foo: "bar"
            }
        }
    }
])
