$version: "2.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@httpRequestTests([
    {
        id: "sends_requests_to_slash",
        protocol: awsJson1_1,
        documentation: "Sends requests to /",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.EmptyOperation",
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "includes_x_amz_target_and_content_type",
        protocol: awsJson1_1,
        documentation: "Includes X-Amz-Target header and Content-Type",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.EmptyOperation",
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "json_1_1_client_sends_empty_payload_for_no_input_shape",
        protocol: awsJson1_1,
        documentation: """
                Clients must always send an empty JSON object payload for
                operations with no input (that is, `{}`). While AWS service
                implementations support requests with no payload or requests
                that send `{}`, always sending `{}` from the client is
                preferred for forward compatibility in case input is ever
                added to an operation.""",
        body: "{}",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.EmptyOperation",
        },
        method: "POST",
        uri: "/"
    },
    {
        id: "json_1_1_service_supports_empty_payload_for_no_input_shape",
        protocol: awsJson1_1,
        documentation: """
                Service implementations must support no payload or an empty
                object payload for operations that define no input. However,
                despite the lack of a payload, a Content-Type header is still
                required in order for the service to properly detect the
                protocol.""",
        body: "",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.EmptyOperation",
        },
        method: "POST",
        uri: "/",
        // Client implementations must ignore this test.
        appliesTo: "server"
    },
])
@httpResponseTests([
    {
        id: "handles_empty_output_shape",
        protocol: awsJson1_1,
        documentation: """
                When no output is defined, the service is expected to return
                an empty payload, however, client must ignore a JSON payload
                if one is returned. This ensures that if output is added later,
                then it will not break the client.""",
        body: "{}",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
        },
        code: 200,
        // Service implementations must ignore this test.
        appliesTo: "client"
    },
    {
        id: "handles_unexpected_json_output",
        protocol: awsJson1_1,
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
            "Content-Type": "application/x-amz-json-1.1",
        },
        code: 200,
        // Service implementations must ignore this test.
        appliesTo: "client"
    },
    {
        id: "json_1_1_service_responds_with_no_payload",
        protocol: awsJson1_1,
        documentation: """
                When no output is defined, the service is expected to return
                an empty payload. Despite the lack of a payload, the service
                is expected to always send a Content-Type header. Clients must
                handle cases where a service returns a JSON object and where
                a service returns no JSON at all.""",
        body: "",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
        },
        code: 200
    },
])
operation EmptyOperation {}
