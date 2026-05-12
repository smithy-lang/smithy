$version: "2.0"

namespace smithy.example

use aws.apigateway#endpointConfiguration

@endpointConfiguration(
    types: ["PRIVATE"]
    ipAddressType: "dualstack"
)
service PrivateDualstackService {
    version: "2024-01-01"
}
