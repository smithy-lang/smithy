// This file defines tests to ensure that implementations support the endpoint
// trait and other features that modify the host.

$version: "2.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests

@httpRequestTests([
    {
        id: "AwsJson11EndpointTrait",
        documentation: """
                Operations can prepend to the given host if they define the
                endpoint trait.""",
        protocol: awsJson1_1,
        method: "POST",
        uri: "/",
        body: "{}",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.EndpointOperation",
        },
        host: "example.com",
        resolvedHost: "foo.example.com",
    }
])
@endpoint(hostPrefix: "foo.")
operation EndpointOperation {}


@httpRequestTests([
    {
        id: "AwsJson11EndpointTraitWithHostLabel",
        documentation: """
                Operations can prepend to the given host if they define the
                endpoint trait, and can use the host label trait to define
                further customization based on user input.""",
        protocol: awsJson1_1,
        method: "POST",
        uri: "/",
        body: "{\"label\": \"bar\"}",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.EndpointWithHostLabelOperation",
        },
        host: "example.com",
        resolvedHost: "foo.bar.example.com",
        params: {
            label: "bar",
        },
    }
])
@endpoint(hostPrefix: "foo.{label}.")
operation EndpointWithHostLabelOperation {
    input: HostLabelInput,
}

structure HostLabelInput {
    @required
    @hostLabel
    label: String,
}
