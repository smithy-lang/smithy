$version: "2.0"

namespace smithy.example

use aws.apigateway#endpointConfiguration

@endpointConfiguration(
    types: ["PRIVATE"]
    ipAddressType: "ipv4"
)
service PrivateIpv4Service {
    version: "2024-01-01"
}
