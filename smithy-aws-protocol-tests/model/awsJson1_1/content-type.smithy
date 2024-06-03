// This file defines test cases that test Content-Type headers.

$version: "2.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// The example tests how servers must support requests
/// containing a `Content-Type` header with parameters.
operation ContentTypeParameters {
    input: ContentTypeParametersInput,
    output: ContentTypeParametersOutput
}

apply ContentTypeParameters @httpRequestTests([
    {
        id: "AwsJson11MustSupportParametersInContentType",
        documentation: "A server should ignore parameters added to the content type",
        protocol: awsJson1_1,
        method: "POST",
        headers: {
            "Content-Type": "application/x-amz-json-1.1; charset=utf-8",
            "X-Amz-Target": "JsonProtocol.ContentTypeParameters",
        },
        uri: "/",
        body: "{\"value\":5}",
        bodyMediaType: "application/json",
        params: {
            value: 5,
        },
        appliesTo: "server"
    }
])

@input
structure ContentTypeParametersInput {
    value: Integer,
}

@output
structure ContentTypeParametersOutput {}
