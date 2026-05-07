$version: "2.0"

namespace smithy.example

use aws.apigateway#gatewayResponses
use aws.protocols#restJson1

@restJson1
@gatewayResponses(
    "DEFAULT_4XX": {
        statusCode: "400"
        responseParameters: {
            "gatewayresponse.header.Access-Control-Allow-Origin": "'*'"
        }
        responseTemplates: {
            "application/json": "{\"message\": \"bad request\"}"
        }
    }
    "DEFAULT_5XX": {
        statusCode: "500"
        responseTemplates: {
            "application/json": "{\"message\": \"Internal server error\"}"
        }
    }
)
service Service {
    version: "2006-03-01"
    operations: [Operation1]
}

@http(uri: "/1", method: "GET")
operation Operation1 {}
