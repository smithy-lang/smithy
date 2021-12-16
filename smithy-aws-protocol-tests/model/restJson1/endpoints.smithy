// This file defines tests to ensure that implementations support the endpoint
// trait and other features that modify the host.

$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpRequestTests

@httpRequestTests([
    {
        id: "RestJsonEndpointTrait",
        documentation: """
                Operations can prepend to the given host if they define the
                endpoint trait.""",
        protocol: restJson1,
        method: "POST",
        uri: "/EndpointOperation",
        body: "",
        host: "example.com",
        resolvedHost: "foo.example.com",
    }
])
@endpoint(hostPrefix: "foo.")
@http(uri: "/EndpointOperation", method: "POST")
operation EndpointOperation {}


@httpRequestTests([
    {
        id: "RestJsonEndpointTraitWithHostLabel",
        documentation: """
                Operations can prepend to the given host if they define the
                endpoint trait, and can use the host label trait to define
                further customization based on user input.""",
        protocol: restJson1,
        method: "POST",
        uri: "/EndpointWithHostLabelOperation",
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
@http(uri: "/EndpointWithHostLabelOperation", method: "POST")
operation EndpointWithHostLabelOperation {
    input: HostLabelInput,
}

structure HostLabelInput {
    @required
    @hostLabel
    label: String,
}
