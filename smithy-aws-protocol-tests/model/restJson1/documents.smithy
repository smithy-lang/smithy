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

@mediaType("application/json")
@exactDocument
string JsonDocument

@mediaType("application/x-foo")
@exactDocument
string SomeBinaryDocument

/// This example serializes exact documents in HTTP headers.
@readonly
@http(uri: "/ExactDocumentInHeader", method: "GET")
operation ExactDocumentInHeader {
    input: ExactDocumentInHeaderInputOutput,
    output: ExactDocumentInHeaderInputOutput
}

structure ExactDocumentInHeaderInputOutput {
    @httpHeader("X-JSON")
    documentValue: JsonDocument,
}

apply ExactDocumentInHeader @httpRequestTests([
    {
        id: "ExactDocumentInHeaderInput",
        documentation: "Serializes an exact document in a base64 encoded header.",
        protocol: restJson1,
        method: "GET",
        uri: "/ExactDocumentInHeader",
        headers: {
            "X-JSON": "eyJncmVldGluZyI6ImhlbGxvIn0="
        },
        params: {
            documentValue: "{\"greeting\": \"hello\"}"
        }
    }
])

apply ExactDocumentInHeader @httpResponseTests([
    {
        id: "ExactDocumentInHeaderInputOutput",
        documentation: "Serializes an exact document in a base64 encoded header.",
        protocol: restJson1,
        code: 200,
        headers: {
            "X-JSON": "eyJncmVldGluZyI6ImhlbGxvIn0="
        },
        params: {
            documentValue: "{\"greeting\": \"hello\"}"
        }
    }
])

/// This example serializes an exact document in a HTTP query string parameter.
@readonly
@http(uri: "/ExactDocumentInQuery", method: "GET")
operation ExactDocumentInQuery {
    input: ExactDocumentInQueryInput
}

structure ExactDocumentInQueryInput {
    @httpQuery("json")
    documentValue: JsonDocument,

    @httpQuery("ion")
    binaryValue: SomeBinaryDocument,
}

apply ExactDocumentInQuery @httpRequestTests([
    {
        id: "ExactDocumentInQueryInput",
        documentation: "Serializes an exact document in a HTTP query string parameter using percent-encoding.",
        protocol: restJson1,
        method: "GET",
        uri: "/ExactDocumentInQuery",
        queryParams: [
            "json=%7B%22greeting%22%3A+%22hello%22%7D",
            "ion=e30K",
        ],
        params: {
            documentValue: "{\"greeting\": \"hello\"}",
            binaryValue: "{}",
        }
    }
])

/// This example serializes exact documents nested inside of request and response payloads.
@idempotent
@http(uri: "/ExactDocumentInPayload", method: "PUT")
operation ExactDocumentInPayload {
    input: ExactDocumentInPayloadInputOutput,
    output: ExactDocumentInPayloadInputOutput
}

structure ExactDocumentInPayloadInputOutput {
    json: JsonDocument,
    binaryValue: SomeBinaryDocument,
}

apply ExactDocumentInPayload @httpRequestTests([
    {
        id: "ExactDocumentInPayloadInput",
        documentation: "Serializes an exact document within the payload of a JSON request.",
        protocol: restJson1,
        method: "PUT",
        uri: "/ExactDocumentInPayload",
        headers: {
            "Content-Type": "application/json"
        },
        body: """
              {
                  "json": "{\"greeting\": \"hello\"}",
                  "ion": "e30K"
              }""",
        bodyMediaType: "application/json",
        params: {
            json: "{\"greeting\": \"hello\"}",
            binaryValue: "{}"
        }
    }
])

apply ExactDocumentInPayload @httpResponseTests([
    {
        id: "ExactDocumentInPayloadOutput",
        documentation: "Serializes an exact document within the payload of a JSON response.",
        protocol: restJson1,
        code: 200,
        headers: {
            "Content-Type": "application/json"
        },
        body: """
              {
                  "json": "{\"greeting\": \"hello\"}",
                  "ion": "e30K"
              }""",
        bodyMediaType: "application/json",
        params: {
            json: "{\"greeting\": \"hello\"}",
            binaryValue: "{}"
        }
    }
])

/// This example serializes an exact string document as the payload of a request and response.
@idempotent
@http(uri: "/ExactStringDocumentAsPayload", method: "PUT")
operation ExactStringDocumentAsPayload {
    input: ExactStringDocumentAsPayloadInputOutput,
    output: ExactStringDocumentAsPayloadInputOutput
}

structure ExactStringDocumentAsPayloadInputOutput {
    @httpPayload
    json: JsonDocument
}

apply ExactStringDocumentAsPayload @httpRequestTests([
    {
        id: "ExactStringDocumentAsPayloadInput",
        documentation: "Serializes an exact string document as the payload of a request.",
        protocol: restJson1,
        method: "PUT",
        uri: "/ExactStringDocumentAsPayload",
        headers: {
            "Content-Type": "application/json"
        },
        body: """
              {"greeting": "hello"}""",
        bodyMediaType: "application/json",
        params: {
            json: "{\"greeting\": \"hello\"}"
        }
    }
])

apply ExactStringDocumentAsPayload @httpResponseTests([
    {
        id: "ExactStringDocumentAsPayloadOutput",
        documentation: "Serializes an exact string document as the payload of a response.",
        protocol: restJson1,
        code: 200,
        headers: {
            "Content-Type": "application/json"
        },
        body: """
              {"greeting": "hello"}""",
        bodyMediaType: "application/json",
        params: {
            json: "{\"greeting\": \"hello\"}"
        }
    }
])

/// This example serializes an exact blob document as the payload of a request and response.
@idempotent
@http(uri: "/ExactBlobDocumentAsPayload", method: "PUT")
operation ExactBlobDocumentAsPayload {
    input: ExactBlobDocumentAsPayloadInputOutput,
    output: ExactBlobDocumentAsPayloadInputOutput
}

structure ExactBlobDocumentAsPayloadInputOutput {
    @httpPayload
    binaryValue: SomeBinaryDocument
}

apply ExactBlobDocumentAsPayload @httpRequestTests([
    {
        id: "ExactBlobDocumentAsPayloadInput",
        documentation: "Serializes an exact blob document as the payload of a request.",
        protocol: restJson1,
        method: "PUT",
        uri: "/ExactBlobDocumentAsPayload",
        headers: {
            // The Content-Type is inherited from the mediaType trait.
            "Content-Type": "application/x-foo"
        },
        body: "{}",
        bodyMediaType: "application/json",
        params: {
            binaryValue: "{}"
        }
    }
])

apply ExactBlobDocumentAsPayload @httpResponseTests([
    {
        id: "ExactBlobDocumentAsPayloadOutput",
        documentation: "Serializes an exact string document as the payload of a response.",
        protocol: restJson1,
        code: 200,
        headers: {
            // The Content-Type is inherited from the mediaType trait.
            "Content-Type": "application/x-foo"
        },
        body: "{}",
        bodyMediaType: "application/json",
        params: {
            binaryValue: "{}"
        }
    }
])
