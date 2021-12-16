// This file defines tests to ensure that implementations support the endpoint
// trait and other features that modify the host.

$version: "2.0"

namespace aws.protocoltests.query

use aws.protocols#awsQuery
use smithy.test#httpRequestTests

@httpRequestTests([
    {
        id: "AwsQueryEndpointTrait",
        documentation: """
                Operations can prepend to the given host if they define the
                endpoint trait.""",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=EndpointOperation&Version=2020-01-08",
        bodyMediaType: "application/x-www-form-urlencoded",
        host: "example.com",
        resolvedHost: "foo.example.com",
    }
])
@endpoint(hostPrefix: "foo.")
operation EndpointOperation {}


@httpRequestTests([
    {
        id: "AwsQueryEndpointTraitWithHostLabel",
        documentation: """
                Operations can prepend to the given host if they define the
                endpoint trait, and can use the host label trait to define
                further customization based on user input.""",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=EndpointWithHostLabelOperation&Version=2020-01-08&label=bar",
        bodyMediaType: "application/x-www-form-urlencoded",
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
