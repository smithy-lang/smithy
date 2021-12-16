// This file defines test cases that test both the HTTP body
// and content-type handling.

$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use aws.protocoltests.shared#TextPlainBlob
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This example operation serializes a structure in the HTTP body.
///
/// It should ensure Content-Type: application/json is
/// used in all requests and that an "empty" body is
/// an empty JSON document ({}).
///
@idempotent
@http(uri: "/body", method: "POST")
operation TestBodyStructure {
    input: TestBodyStructureInputOutput,
    output: TestBodyStructureInputOutput
}

apply TestBodyStructure @httpRequestTests([
    {
        id: "RestJsonTestBodyStructure",
        documentation: "Serializes a structure",
        protocol: restJson1,
        method: "POST",
        uri: "/body",
        body: """
              {"testConfig":
                  {"timeout": 10}
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            testConfig: {
                timeout: 10,
            }
        }
    }
])

apply TestBodyStructure @httpRequestTests([
    {
        id: "RestJsonHttpWithEmptyBody",
        documentation: "Serializes an empty structure in the body",
        protocol: restJson1,
        method: "POST",
        uri: "/body",
        body: "{}",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {}
    }
])

structure TestBodyStructureInputOutput {
    @httpHeader("x-amz-test-id")
    testId: String,

    testConfig: TestConfig
}

structure TestConfig {
    timeout: Integer
}

/// This example operation serializes a payload targeting a structure.
///
/// This enforces the same requirements as TestBodyStructure
/// but with the body specified by the @httpPayload trait.
///
@idempotent
@http(uri: "/payload", method: "POST")
operation TestPayloadStructure {
    input: TestPayloadStructureInputOutput,
    output: TestPayloadStructureInputOutput
}

apply TestPayloadStructure @httpRequestTests([
    {
        id: "RestJsonHttpWithEmptyStructurePayload",
        documentation: "Serializes a payload targeting an empty structure",
        protocol: restJson1,
        method: "POST",
        uri: "/payload",
        body: "{}",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {}
    }
])

apply TestPayloadStructure @httpRequestTests([
    {
        id: "RestJsonTestPayloadStructure",
        documentation: "Serializes a payload targeting a structure",
        protocol: restJson1,
        method: "POST",
        uri: "/payload",
        body: """
              {"data": 25
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            payloadConfig: {
                data: 25,
            }
        }
    }
])

apply TestPayloadStructure @httpRequestTests([
    {
        id: "RestJsonHttpWithHeadersButNoPayload",
        documentation: "Serializes an request with header members but no payload",
        protocol: restJson1,
        method: "POST",
        uri: "/payload",
        body: "{}",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json",
            "X-Amz-Test-Id": "t-12345"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            testId: "t-12345"
        }
    }
])

structure TestPayloadStructureInputOutput {
    @httpHeader("x-amz-test-id")
    testId: String,

    @httpPayload
    payloadConfig: PayloadConfig
}

structure PayloadConfig {
    data: Integer
}

/// This example operation serializes a payload targeting a blob.
///
/// The Blob shape is not structured content and we cannot
/// make assumptions about what data will be sent. This test ensures
/// only a generic "Content-Type: application/octet-stream" header
/// is used, and that we are not treating an empty body as an
/// empty JSON document.
///
@idempotent
@http(uri: "/blob_payload", method: "POST")
operation TestPayloadBlob {
    input: TestPayloadBlobInputOutput,
    output: TestPayloadBlobInputOutput
}

apply TestPayloadBlob @httpRequestTests([
    {
        id: "RestJsonHttpWithEmptyBlobPayload",
        documentation: "Serializes a payload targeting an empty blob",
        protocol: restJson1,
        method: "POST",
        uri: "/blob_payload",
        body: "",
        bodyMediaType: "application/octet-stream",
        headers: {
            "Content-Type": "application/octet-stream"
        },
        params: {}
    }
])

apply TestPayloadBlob @httpRequestTests([
    {
        id: "RestJsonTestPayloadBlob",
        documentation: "Serializes a payload targeting a blob",
        protocol: restJson1,
        method: "POST",
        uri: "/blob_payload",
        body: "1234",
        bodyMediaType: "image/jpg",
        headers: {
            "Content-Type": "image/jpg"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            contentType: "image/jpg",
            data: "1234"
        }
    }
])

structure TestPayloadBlobInputOutput {
    @httpHeader("Content-Type")
    contentType: String,

    @httpPayload
    data: Blob
}

/// This example operation serializes a request without an HTTP body.
///
/// These tests are to ensure we do not attach a body or related headers
/// (Content-Length, Content-Type) to operations that semantically
/// cannot produce an HTTP body.
///
@readonly
@http(uri: "/no_payload", method: "GET")
operation TestNoPayload {
    input: TestNoPayloadInputOutput,
    output: TestNoPayloadInputOutput
}

apply TestNoPayload @httpRequestTests([
    {
        id: "RestJsonHttpWithNoModeledBody",
        documentation: "Serializes a GET request with no modeled body",
        protocol: restJson1,
        method: "GET",
        uri: "/no_payload",
        body: "",
        forbidHeaders: [
            "Content-Length",
            "Content-Type"
        ],
        params: {}
    }
])

apply TestNoPayload @httpRequestTests([
    {
        id: "RestJsonHttpWithHeaderMemberNoModeledBody",
        documentation: "Serializes a GET request with header member but no modeled body",
        protocol: restJson1,
        method: "GET",
        uri: "/no_payload",
        body: "",
        headers: {
            "X-Amz-Test-Id": "t-12345"
        },
        forbidHeaders: [
            "Content-Length",
            "Content-Type"
        ],
        params: {
            testId: "t-12345"
        }
    }
])

structure TestNoPayloadInputOutput {
    @httpHeader("X-Amz-Test-Id")
    testId: String,
}
