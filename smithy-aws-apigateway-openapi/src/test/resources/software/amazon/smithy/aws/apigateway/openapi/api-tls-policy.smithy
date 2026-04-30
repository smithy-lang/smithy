$version: "2.0"

namespace smithy.example

use aws.apigateway#apiTlsPolicy
use aws.protocols#restJson1

@restJson1
@apiTlsPolicy(
    securityPolicy: "TLS_1_2"
    endpointAccessMode: "STRICT"
)
service Service {
    version: "2006-03-01"
    operations: [Operation1]
}

@http(uri: "/1", method: "GET")
operation Operation1 {}
