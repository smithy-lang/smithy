// Endpoint traits are not currently supported.
$version: "2.0"

namespace smithy.example

@aws.protocols#restJson1
service EndpointService {
  version: "2018-01-01",
  operations: [EndpointOperation]
}

@http(method: "GET", uri: "/")
@readonly
@endpoint(hostPrefix: "prefix.")
operation EndpointOperation {}
