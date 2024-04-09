// This file defines tests to ensure that implementations support the endpoint
// trait and other features that modify the host.

$version: "2.0"
$operationInputSuffix: "Request"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use smithy.test#httpRequestTests

@httpRequestTests([
    {
        id: "RestXmlEndpointTrait",
        documentation: """
                Operations can prepend to the given host if they define the
                endpoint trait.""",
        protocol: restXml,
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
        id: "RestXmlEndpointTraitWithHostLabel",
        documentation: """
                Operations can prepend to the given host if they define the
                endpoint trait, and can use the host label trait to define
                further customization based on user input.""",
        protocol: restXml,
        method: "POST",
        uri: "/EndpointWithHostLabelOperation",
        body: """
              <EndpointWithHostLabelOperationRequest>
                  <label>bar</label>
              </EndpointWithHostLabelOperationRequest>
              """,
        bodyMediaType: "application/xml",
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
    input := {
        @required
        @hostLabel
        label: String
    }
}

@httpRequestTests([
    {
        id: "RestXmlEndpointTraitWithHostLabelAndHttpBinding",
        documentation: """
                Operations can prepend to the given host if they define the
                endpoint trait, and can use the host label trait to define
                further customization based on user input. The label must also
                be serialized in into any other location it is bound to, such
                as the body or in this case an http header.""",
        protocol: restXml,
        method: "POST",
        uri: "/EndpointWithHostLabelHeaderOperation",
        body: "",
        bodyMediaType: "application/xml",
        host: "example.com",
        resolvedHost: "bar.example.com",
        headers: {
            "X-Amz-Account-Id": "bar",
        },
        params: {
            accountId: "bar",
        },
    }
])
@endpoint(hostPrefix: "{accountId}.")
@http(uri: "/EndpointWithHostLabelHeaderOperation", method: "POST")
operation EndpointWithHostLabelHeaderOperation {
    input: HostLabelHeaderInput,
}

structure HostLabelHeaderInput {
    @required
    @hostLabel
    @httpHeader("X-Amz-Account-Id")
    accountId: String,
}
