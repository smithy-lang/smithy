$version: "2.0"

namespace smithy.example

use aws.apigateway#gatewayResponses
use aws.protocols#restJson1

@suppress(["GatewayResponsesCors"])
@restJson1
@cors(origin: "https://cors-default.example.com")
@gatewayResponses(
    "DEFAULT_4XX": {
        statusCode: "400"
        responseParameters: {
            "gatewayresponse.header.Access-Control-Allow-Origin": "'https://custom.example.com'"
        }
    }
)
service Service {
    version: "2006-03-01"
    operations: [Operation1]
}

@http(uri: "/1", method: "GET")
operation Operation1 {}
