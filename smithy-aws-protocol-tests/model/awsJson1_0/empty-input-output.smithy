// This file defines test cases that test the basics of empty input and
// output shape serialization.

$version: "2.0"

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
        id: "AwsJson10MustAlwaysSendEmptyJsonPayload",
        documentation: """
                Clients must always send an empty JSON object payload for
                operations with no input (that is, `{}`). While AWS service
                implementations support requests with no payload or requests
                that send `{}`, always sending `{}` from the client is
                preferred for forward compatibility in case input is ever
                added to an operation.""",
        protocol: awsJson1_0,
        method: "POST",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.NoInputAndNoOutput",
        },
        uri: "/",
        body: "{}",
        bodyMediaType: "application/json"
    },
    {
        id: "AwsJson10ServiceSupportsNoPayloadForNoInput",
        documentation: """
                Service implementations must support no payload or an empty
                object payload for operations that define no input. However,
                despite the lack of a payload, a Content-Type header is still
                required in order for the service to properly detect the
                protocol.""",
        protocol: awsJson1_0,
        method: "POST",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.NoInputAndNoOutput",
        },
        uri: "/",
        body: "",
        appliesTo: "server"
    },
])

apply NoInputAndNoOutput @httpResponseTests([
    {
        id: "AwsJson10HandlesEmptyOutputShape",
        protocol: awsJson1_0,
        documentation: """
                When no output is defined, the service is expected to return
                an empty payload, however, client must ignore a JSON payload
                if one is returned. This ensures that if output is added later,
                then it will not break the client.""",
        body: "{}",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
        },
        code: 200,
        // Service implementations must ignore this test.
        appliesTo: "client"
    },
    {
        id: "AwsJson10HandlesUnexpectedJsonOutput",
        protocol: awsJson1_0,
        documentation: """
                This client-only test builds on handles_empty_output_shape,
                by including unexpected fields in the JSON. A client
                needs to ignore JSON output that is empty or that contains
                JSON object data.""",
        body: """
            {
                "foo": true
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
        },
        code: 200,
        // Service implementations must ignore this test.
        appliesTo: "client"
    },
    {
        id: "AwsJson10ServiceRespondsWithNoPayload",
        protocol: awsJson1_0,
        documentation: """
                When no output is defined, the service is expected to return
                an empty payload. Despite the lack of a payload, the service
                is expected to always send a Content-Type header. Clients must
                handle cases where a service returns a JSON object and where
                a service returns no JSON at all.""",
        body: "",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
        },
        code: 200
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
        documentation: "A client should always send and empty JSON object payload.",
        protocol: awsJson1_0,
        method: "POST",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.NoInputAndOutput",
        },
        uri: "/",
        body: "{}",
        bodyMediaType: "application/json",
    }
])

apply NoInputAndOutput @httpResponseTests([
    {
        id: "AwsJson10NoInputAndOutput",
        documentation: "Empty output always serializes an empty object payload.",
        protocol: awsJson1_0,
        headers: {
            "Content-Type": "application/x-amz-json-1.0"
        },
        code: 200,
        body: "{}",
        bodyMediaType: "application/json",
    }
])

@output
structure NoInputAndOutputOutput {}

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has an empty input
/// and empty output structure that reuses the same shape. While this should
/// be rare, code generators must support this.
operation EmptyInputAndEmptyOutput {
    input: EmptyInputAndEmptyOutputInput,
    output: EmptyInputAndEmptyOutputOutput
}

apply EmptyInputAndEmptyOutput @httpRequestTests([
    {
        id: "AwsJson10EmptyInputAndEmptyOutput",
        documentation: "Clients must always send an empty object if input is modeled.",
        protocol: awsJson1_0,
        method: "POST",
        uri: "/",
        body: "{}",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.EmptyInputAndEmptyOutput",
        }
    },
])

apply EmptyInputAndEmptyOutput @httpResponseTests([
    {
        id: "AwsJson10EmptyInputAndEmptyOutputSendJsonObject",
        documentation: "A service will always return a JSON object for operations with modeled output.",
        protocol: awsJson1_0,
        code: 200,
        body: "{}",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0"
        },
    },
])

@input
structure EmptyInputAndEmptyOutputInput {}

@output
structure EmptyInputAndEmptyOutputOutput {}
