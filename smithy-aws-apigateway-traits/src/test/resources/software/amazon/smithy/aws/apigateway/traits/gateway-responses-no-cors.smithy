$version: "2.0"

namespace smithy.example

use aws.apigateway#gatewayResponses

@gatewayResponses(
    "DEFAULT_4XX": {
        statusCode: "400"
    }
)
service Service {
    version: "2006-03-01"
}
