// This file defines test cases that serialize inline documents.

$version: "2.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This example serializes an inline document as part of the payload.
operation PutAndGetInlineDocuments {
    input: PutAndGetInlineDocumentsInputOutput,
    output: PutAndGetInlineDocumentsInputOutput
}

structure PutAndGetInlineDocumentsInputOutput {
    inlineDocument: Document
}

document Document

apply PutAndGetInlineDocuments @httpRequestTests([
    {
        id: "PutAndGetInlineDocumentsInput",
        documentation: "Serializes inline documents in a JSON request.",
        protocol: awsJson1_1,
        method: "POST",
        uri: "/",
        body: """
              {
                  "inlineDocument": {"foo": "bar"}
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.PutAndGetInlineDocuments",
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            inlineDocument: {
                foo: "bar"
            }
        }
    }
])

apply PutAndGetInlineDocuments @httpResponseTests([
    {
        id: "PutAndGetInlineDocumentsInput",
        documentation: "Serializes inline documents in a JSON response.",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "inlineDocument": {"foo": "bar"}
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/x-amz-json-1.1"},
        params: {
            inlineDocument: {
                foo: "bar"
            }
        }
    }
])
