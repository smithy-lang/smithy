// This file defines test cases that serialize document types.

$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

// Define some shapes shared throughout these test cases.
document Document

/// This example serializes a document as part of the payload.
@idempotent
@http(uri: "/DocumentType", method: "PUT")
operation DocumentType {
    input: DocumentTypeInputOutput,
    output: DocumentTypeInputOutput
}

structure DocumentTypeInputOutput {
    stringValue: String,
    documentValue: Document,
}

apply DocumentType @httpRequestTests([
    {
        id: "DocumentTypeInputWithObject",
        documentation: "Serializes document types as part of the JSON request payload with no escaping.",
        protocol: restJson1,
        method: "PUT",
        uri: "/DocumentType",
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
    },
    {
        id: "DocumentInputWithString",
        documentation: "Serializes document types using a string.",
        protocol: restJson1,
        method: "PUT",
        uri: "/DocumentType",
        body: """
              {
                  "stringValue": "string",
                  "documentValue": "hello"
              }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            stringValue: "string",
            documentValue: "hello"
        }
    },
    {
        id: "DocumentInputWithNumber",
        documentation: "Serializes document types using a number.",
        protocol: restJson1,
        method: "PUT",
        uri: "/DocumentType",
        body: """
              {
                  "stringValue": "string",
                  "documentValue": 10
              }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            stringValue: "string",
            documentValue: 10
        }
    },
    {
        id: "DocumentInputWithBoolean",
        documentation: "Serializes document types using a boolean.",
        protocol: restJson1,
        method: "PUT",
        uri: "/DocumentType",
        body: """
              {
                  "stringValue": "string",
                  "documentValue": true
              }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            stringValue: "string",
            documentValue: true
        }
    },
    {
        id: "DocumentInputWithList",
        documentation: "Serializes document types using a list.",
        protocol: restJson1,
        method: "PUT",
        uri: "/DocumentType",
        body: """
              {
                  "stringValue": "string",
                  "documentValue": [
                      true,
                      "hi",
                      [
                          1,
                          2
                      ],
                      {
                          "foo": {
                              "baz": [
                                  3,
                                  4
                              ]
                          }
                      }
                  ]
              }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            stringValue: "string",
            documentValue: [
                true,
                "hi",
                [
                    1,
                    2
                ],
                {
                    "foo": {
                        "baz": [
                            3,
                            4
                        ]
                    }
                }
            ]
        }
    },
])

apply DocumentType @httpResponseTests([
    {
        id: "DocumentOutput",
        documentation: "Serializes documents as part of the JSON response payload with no escaping.",
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
    },
    {
        id: "DocumentOutputString",
        documentation: "Document types can be JSON scalars too.",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "stringValue": "string",
                "documentValue": "hello"
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            stringValue: "string",
            documentValue: "hello"
        }
    },
    {
        id: "DocumentOutputNumber",
        documentation: "Document types can be JSON scalars too.",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "stringValue": "string",
                "documentValue": 10
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            stringValue: "string",
            documentValue: 10
        }
    },
    {
        id: "DocumentOutputBoolean",
        documentation: "Document types can be JSON scalars too.",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "stringValue": "string",
                "documentValue": false
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            stringValue: "string",
            documentValue: false
        }
    },
    {
        id: "DocumentOutputArray",
        documentation: "Document types can be JSON arrays.",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "stringValue": "string",
                "documentValue": [
                    true,
                    false
                ]
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            stringValue: "string",
            documentValue: [true, false]
        }
    }
])

/// This example serializes a document as the entire HTTP payload.
@idempotent
@http(uri: "/DocumentTypeAsPayload", method: "PUT")
operation DocumentTypeAsPayload {
    input: DocumentTypeAsPayloadInputOutput,
    output: DocumentTypeAsPayloadInputOutput
}

structure DocumentTypeAsPayloadInputOutput {
    @httpPayload
    documentValue: Document,
}

apply DocumentTypeAsPayload @httpRequestTests([
    {
        id: "DocumentTypeAsPayloadInput",
        documentation: "Serializes a document as the target of the httpPayload trait.",
        protocol: restJson1,
        method: "PUT",
        uri: "/DocumentTypeAsPayload",
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
    },
    {
        id: "DocumentTypeAsPayloadInputString",
        documentation: "Serializes a document as the target of the httpPayload trait using a string.",
        protocol: restJson1,
        method: "PUT",
        uri: "/DocumentTypeAsPayload",
        body: "\"hello\"",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            documentValue: "hello"
        }
    }
])

apply DocumentTypeAsPayload @httpResponseTests([
    {
        id: "DocumentTypeAsPayloadOutput",
        documentation: "Serializes a document as the target of the httpPayload trait.",
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
    },
    {
        id: "DocumentTypeAsPayloadOutputString",
        documentation: "Serializes a document as a payload string.",
        protocol: restJson1,
        code: 200,
        body: "\"hello\"",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            documentValue: "hello"
        }
    }
])

/// This example serializes documents as the value of maps.
@idempotent
@http(uri: "/DocumentTypeAsMapValue", method: "PUT")
operation DocumentTypeAsMapValue {
    input: DocumentTypeAsMapValueInputOutput,
    output: DocumentTypeAsMapValueInputOutput,
}

structure DocumentTypeAsMapValueInputOutput {
    docValuedMap: DocumentValuedMap,
}

map DocumentValuedMap {
    key: String,
    value: Document,
}

apply DocumentTypeAsMapValue @httpRequestTests([
    {
        id: "DocumentTypeAsMapValueInput",
        documentation: "Serializes a map that uses documents as the value.",
        protocol: restJson1,
        method: "PUT",
        uri: "/DocumentTypeAsMapValue",
        body: """
            {
                "docValuedMap": {
                    "foo": { "f": 1, "o": 2 },
                    "bar": [ "b", "a", "r" ],
                    "baz": "BAZ"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            docValuedMap: {
                "foo": { "f": 1, "o": 2 },
                "bar": [ "b", "a", "r" ],
                "baz": "BAZ",
            },
        },
    },
])

apply DocumentTypeAsMapValue @httpResponseTests([
    {
        id: "DocumentTypeAsMapValueOutput",
        documentation: "Serializes a map that uses documents as the value.",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "docValuedMap": {
                    "foo": { "f": 1, "o": 2 },
                    "bar": [ "b", "a", "r" ],
                    "baz": "BAZ"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            docValuedMap: {
                "foo": { "f": 1, "o": 2 },
                "bar": [ "b", "a", "r" ],
                "baz": "BAZ",
            },
        },
    },
])
