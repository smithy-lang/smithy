// This file defines tests to ensure that implementations support the endpoint
// trait and other features that modify the host.

$version: "2.0"

namespace aws.protocoltests.json10

use aws.protocols#awsJson1_0
use smithy.test#httpRequestTests

@httpRequestTests([
    {
        id: "AwsJson10EndpointTrait",
        documentation: """
                Operations can prepend to the given host if they define the
                endpoint trait.""",
        protocol: awsJson1_0,
        method: "POST",
        uri: "/",
        body: "{}",
        host: "example.com",
        resolvedHost: "foo.example.com",
    }
])
@endpoint(hostPrefix: "foo.")
operation EndpointOperation {}


@httpRequestTests([
    {
        id: "AwsJson10EndpointTraitWithHostLabel",
        documentation: """
                Operations can prepend to the given host if they define the
                endpoint trait, and can use the host label trait to define
                further customization based on user input.""",
        protocol: awsJson1_0,
        method: "POST",
        uri: "/",
        body: "{\"label\": \"bar\"}",
        bodyMediaType: "application/json",
        host: "example.com",
        resolvedHost: "foo.bar.example.com",
        params: {
            label: "bar",
        },
    }
])
@endpoint(hostPrefix: "foo.{label}.")
operation EndpointWithHostLabelOperation {
    input: EndpointWithHostLabelOperationInput,
}

@input
structure EndpointWithHostLabelOperationInput {
    @required
    @hostLabel
    label: String,
}
