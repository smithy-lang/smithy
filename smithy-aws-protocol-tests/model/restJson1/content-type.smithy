// This file defines test cases that test Content-Type headers.

$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use aws.protocoltests.shared#DateTime
use smithy.test#httpRequestTests

/// The example tests how servers must support requests
/// containing a `Content-Type` header with parameters.
@http(uri: "/ContentTypeParameters", method: "POST")
operation ContentTypeParameters {
    input: ContentTypeParametersInput,
    output: ContentTypeParametersOutput,
}

apply ContentTypeParameters @httpRequestTests([
    {
        id: "RestJsonMustSupportParametersInContentType",
        documentation: "A server should ignore parameters added to the content type",
        uri: "/ContentTypeParameters",
        method: "POST",
        protocol: "aws.protocols#restJson1",
        body: "{\"value\":5}",
        headers: { "Content-Type": "application/json; charset=utf-8" },
        params: {
            value: 5,
        },
        appliesTo: "server",
    },
])

@input
structure ContentTypeParametersInput {
    value: Integer,
}

@output
structure ContentTypeParametersOutput {}
