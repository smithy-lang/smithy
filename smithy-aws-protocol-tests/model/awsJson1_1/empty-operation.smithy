$version: "1.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@httpRequestTests([
    {
        id: "sends_requests_to_slash",
        protocol: awsJson1_1,
        documentation: "Sends requests to /",
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
])
@httpResponseTests([
    {
        id: "handles_empty_output_shape",
        protocol: awsJson1_1,
        documentation: "Handles empty output shapes",
        body: "{}",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
        },
        code: 200,
    },
])
operation EmptyOperation {}
