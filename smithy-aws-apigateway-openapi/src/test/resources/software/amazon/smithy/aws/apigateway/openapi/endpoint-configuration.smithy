$version: "2.0"

namespace smithy.example

use aws.apigateway#endpointConfiguration
use aws.protocols#restJson1

@restJson1
@endpointConfiguration(
    types: ["PRIVATE"]
    vpcEndpointIds: ["vpce-0212a4ababd5b8c3e", "vpce-01d622316a7df47f9"]
    disableExecuteApiEndpoint: true
    ipAddressType: "dualstack"
)
service Service {
    version: "2006-03-01"
    operations: [Operation]
}

@http(uri: "/", method: "GET")
operation Operation {}
