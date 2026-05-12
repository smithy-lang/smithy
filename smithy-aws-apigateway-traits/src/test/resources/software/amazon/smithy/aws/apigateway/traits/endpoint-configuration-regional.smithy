$version: "2.0"

namespace smithy.example

use aws.apigateway#endpointConfiguration

@endpointConfiguration(
    types: ["REGIONAL"]
    ipAddressType: "ipv4"
)
service RegionalIpv4Service {
    version: "2024-01-01"
}
