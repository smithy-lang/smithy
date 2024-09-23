// This file defines test cases that test Content-Type headers.

$version: "2.0"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use aws.protocoltests.shared#DateTime
use smithy.test#httpRequestTests

/// The example tests how servers must support requests
/// containing a `Content-Type` header with parameters.
@idempotent
@http(uri: "/ContentTypeParameters", method: "PUT")
operation ContentTypeParameters {
    input: ContentTypeParametersInput,
    output: ContentTypeParametersOutput
}

apply ContentTypeParameters @httpRequestTests([
    {
        id: "RestXmlMustSupportParametersInContentType",
        documentation: "A server should ignore parameters added to the content type",
        protocol: restXml,
        method: "PUT",
        headers: {
            "Content-Type": "application/xml; charset=utf-8"
        },
        uri: "/ContentTypeParameters",
        body: "<ContentTypeParametersInput><value>5</value></ContentTypeParametersInput>",
        bodyMediaType: "application/xml",
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
