$version: "2.0"

namespace smithy.example

use aws.apigateway#endpointConfiguration
use aws.protocols#restJson1

/// Only required `types` is set. No extension should be emitted because
/// `types` and `ipAddressType` are not written to
/// `x-amazon-apigateway-endpoint-configuration`.
@restJson1
@endpointConfiguration(types: ["REGIONAL"])
service Service {
    version: "2006-03-01"
    operations: [Operation]
}

@http(uri: "/", method: "GET")
operation Operation {}
