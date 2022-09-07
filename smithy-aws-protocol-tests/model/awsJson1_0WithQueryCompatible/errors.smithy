$version: "2.0"

namespace aws.protocoltests.json10

use aws.protocols#awsJson1_0
use aws.protocols#awsQueryError
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@idempotent
operation GreetingWithError {
    input: GreetingWithErrorInput,
    output: GreetingWithErrorOutput,
    errors: [InvalidGreetingError]
}

@input
structure GreetingWithErrorInput {
    greeting: String,
}

@output
structure GreetingWithErrorOutput {
    greeting: String,
}

@awsQueryError(
    code: "CustomGreetingErrorCode",
    httpResponseCode: 402
)
@error("client")
structure InvalidGreetingError {
    Message: String,
}

apply InvalidGreeting @httpResponseTests([
    {
        id: "Json10WithQueryCompatibleGreetingError",
        documentation: "@awsQueryCompatible trait is applied to service",
        protocol: awsJson1_0,
        params: {
            Message: "Hi"
        },
        code: 402,
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
            "x-amzn-query-error": "CustomGreetingErrorCode;Sender"
        },
        bodyMediaType: "application/json",
        body: "{\"__type\": \"InvalidGreetingError\",\"Message\": \"Hi\"}",
        appliesTo: "client"
    },
])
