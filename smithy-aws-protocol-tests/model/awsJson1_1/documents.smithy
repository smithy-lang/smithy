// This file defines test cases that serialize inline and exact documents.

$version: "1.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This example serializes an inline document as part of the payload.
operation PutAndGetDocuments {
    input: PutAndGetDocumentsInputOutput,
    output: PutAndGetDocumentsInputOutput
}

structure PutAndGetDocumentsInputOutput {
    inlineDocument: Document,
    jsonValue: JsonDocument,
    ionValue: IonBinaryDocument,
}

document Document

@exactDocument
@mediaType("application/json")
string JsonDocument

@exactDocument
@mediaType("application/x-amzn-ion")
blob IonBinaryDocument

apply PutAndGetDocuments @httpRequestTests([
    {
        id: "PutAndGetDocumentsInput",
        documentation: "Serializes inline and exact documents in a JSON request.",
        protocol: awsJson1_1,
        method: "POST",
        uri: "/",
        body: """
              {
                  "inlineDocument": {"foo": "bar"},
                  "jsonValue": "[\"hi\"]",
                  "ionValue": "e30K",
              }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            inlineDocument: {"foo": "bar"},
            jsonValue: "{}",
            // This is not valid binary Ion, but the point here is that the
            // value is base64 encoded (just like any other blob).
            ionValue: "{}",
        }
    }
])

apply PutAndGetDocuments @httpResponseTests([
    {
        id: "PutAndGetDocumentsInput",
        documentation: "Serializes inline and exact documents in a JSON response.",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "inlineDocument": {"foo": "bar"},
                "jsonValue": "[\"hi\"]",
                "ionValue": "e30K",
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            inlineDocument: {"foo": "bar"},
            jsonValue: "[\"hi\"]",
            // This is not valid binary Ion, but the point here is that the
            // value is base64 encoded (just like any other blob).
            ionValue: "{}",
        }
    }
])
