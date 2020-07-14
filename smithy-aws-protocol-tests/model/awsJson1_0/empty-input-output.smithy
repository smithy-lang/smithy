// This file defines test cases that test the basics of empty input and
// output shape serialization.

$version: "1.0"

namespace aws.protocoltests.json10

use aws.protocols#awsJson1_0
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has no input or output.
/// While this should be rare, code generators must support this.
operation NoInputAndNoOutput {}

apply NoInputAndNoOutput @httpRequestTests([
    {
        id: "AwsJson10NoInputAndNoOutput",
        documentation: "No input serializes no payload",
        protocol: awsJson1_0,
        method: "POST",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.NoInputAndNoOutput",
        },
        uri: "/",
    }
])

apply NoInputAndNoOutput @httpResponseTests([
   {
        id: "AwsJson10NoInputAndNoOutput",
        documentation: "No output serializes no payload",
        protocol: awsJson1_0,
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
        },
        code: 200,
   }
])

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has no input and the
/// output is empty. While this should be rare, code generators must support
/// this.
operation NoInputAndOutput {
    output: NoInputAndOutputOutput
}

apply NoInputAndOutput @httpRequestTests([
    {
        id: "AwsJson10NoInputAndOutput",
        documentation: "No input serializes no payload",
        protocol: awsJson1_0,
        method: "POST",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.NoInputAndOutput",
        },
        uri: "/"
    }
])

apply NoInputAndOutput @httpResponseTests([
    {
        id: "AwsJson10NoInputAndOutput",
        documentation: "Empty output serializes no payload",
        protocol: awsJson1_0,
        headers: {
            "Content-Type": "application/x-amz-json-1.0"
        },
        code: 200
    }
])

structure NoInputAndOutputOutput {}

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has an empty input
/// and empty output structure that reuses the same shape. While this should
/// be rare, code generators must support this.
operation EmptyInputAndEmptyOutput {
    input: EmptyInputAndEmptyOutputInput,
    output: EmptyInputAndEmptyOutputInput
}

apply EmptyInputAndEmptyOutput @httpRequestTests([
    {
        id: "AwsJson10EmptyInputAndEmptyOutput",
        documentation: "Empty input serializes no payload",
        protocol: awsJson1_0,
        method: "POST",
        uri: "/",
        body: "",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.EmptyInputAndEmptyOutput",
        },
        bodyMediaType: "application/json"
    },
])

apply EmptyInputAndEmptyOutput @httpResponseTests([
    {
        id: "AwsJson10EmptyInputAndEmptyOutput",
        documentation: "Empty output serializes no payload",
        protocol: awsJson1_0,
        code: 200,
        body: "",
        headers: {"Content-Type": "application/x-amz-json-1.0"},
        bodyMediaType: "application/json"
    },
    {
        id: "AwsJson10EmptyInputAndEmptyJsonObjectOutput",
        documentation: "Empty output serializes no payload",
        protocol: awsJson1_0,
        code: 200,
        body: "{}",
        headers: {"Content-Type": "application/x-amz-json-1.0"},
        bodyMediaType: "application/json"
    },
])

structure EmptyInputAndEmptyOutputInput {}
structure EmptyInputAndEmptyOutputOutput {}
