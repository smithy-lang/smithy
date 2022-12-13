$version: "2.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@httpRequestTests([
    {
        id: "can_call_operation_with_no_input_or_output",
        protocol: awsJson1_1,
        documentation: "Can call operations with no input or output",
        body: "{}",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.OperationWithOptionalInputOutput",
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "can_call_operation_with_optional_input",
        protocol: awsJson1_1,
        documentation: "Can invoke operations with optional input",
        body: "{\"Value\":\"Hi\"}",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.OperationWithOptionalInputOutput",
        },
        params: {
            Value: "Hi",
        },
        method: "POST",
        uri: "/",
    },
])
operation OperationWithOptionalInputOutput {
    input: OperationWithOptionalInputOutputInput,
    output: OperationWithOptionalInputOutputOutput,
}

@input
structure OperationWithOptionalInputOutputInput {
    Value: String
}

@output
structure OperationWithOptionalInputOutputOutput {
    Value: String
}
