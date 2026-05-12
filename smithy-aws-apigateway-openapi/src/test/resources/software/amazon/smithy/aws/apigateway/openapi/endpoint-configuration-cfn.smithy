$version: "2.0"

namespace smithy.example

use aws.apigateway#endpointConfiguration
use aws.protocols#restJson1

@restJson1
@endpointConfiguration(
    types: ["PRIVATE"]
    vpcEndpointIds: ["${MyVpcEndpointId}"]
    disableExecuteApiEndpoint: true
)
service Service {
    version: "2006-03-01"
    operations: [Operation]
}

@http(uri: "/", method: "GET")
operation Operation {}
