// This file defines tests to ensure that implementations support the endpoint
// trait and other features that modify the host.

$version: "2.0"

namespace aws.protocoltests.ec2

use aws.protocols#ec2Query
use smithy.test#httpRequestTests

@httpRequestTests([
    {
        id: "Ec2QueryEndpointTrait",
        documentation: """
                Operations can prepend to the given host if they define the
                endpoint trait.""",
        protocol: ec2Query,
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
        id: "Ec2QueryEndpointTraitWithHostLabel",
        documentation: """
                Operations can prepend to the given host if they define the
                endpoint trait, and can use the host label trait to define
                further customization based on user input.""",
        protocol: ec2Query,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=EndpointWithHostLabelOperation&Version=2020-01-08&Label=bar",
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
