// This file defines test cases that serialize inline documents.

$version: "1.0"

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
        headers: {"Content-Type": "application/json"},
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
        headers: {"Content-Type": "application/json"},
        params: {
            inlineDocument: {
                foo: "bar"
            }
        }
    }
])

/// This example serializes an exact document in JSON input and output.
operation PutAndGetExactDocuments {
    input: PutAndGetExactDocumentsInputOutput,
    output: PutAndGetExactDocumentsInputOutput
}

structure PutAndGetExactDocumentsInputOutput {
    jsonValue: JsonDocument,
    binaryDocument: SomeBinaryDocument,
}

@exactDocument
@mediaType("application/json")
string JsonDocument

@exactDocument
@mediaType("application/x-foo")
blob SomeBinaryDocument

apply PutAndGetExactDocuments @httpRequestTests([
    {
        id: "PutAndGetExactDocumentsInput",
        documentation: "Serializes exact documents in a JSON request.",
        protocol: awsJson1_1,
        method: "POST",
        uri: "/",
        body: """
              {
                  "jsonValue": "[\"hi\"]",
                  "binaryDocument": "e30K"
              }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            jsonValue: "{}",
            binaryDocument: "{}",
        }
    }
])

apply PutAndGetExactDocuments @httpResponseTests([
    {
        id: "PutAndGetExactDocumentsOutput",
        documentation: "Serializes exact documents in a JSON response.",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "jsonValue": "[\"hi\"]",
                "binaryDocument": "e30K"
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            jsonValue: "[\"hi\"]",
            binaryDocument: "{}",
        }
    }
])
