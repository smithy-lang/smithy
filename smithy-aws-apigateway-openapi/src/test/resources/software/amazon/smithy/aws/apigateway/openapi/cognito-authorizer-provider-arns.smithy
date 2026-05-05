$version: "2.0"

namespace smithy.example

use aws.apigateway#authorizer
use aws.apigateway#authorizers
use aws.auth#sigv4
use aws.protocols#restJson1

@restJson1
@sigv4(name: "service")
@authorizer("my-cognito-auth")
@authorizers(
    "my-cognito-auth": {
        scheme: "aws.auth#sigv4"
        type: "cognito_user_pools"
        providerARNs: ["arn:aws:cognito-idp:us-east-1:123456789012:userpool/us-east-1_abc123"]
    }
)
service Service {
    version: "2006-03-01"
    operations: [Operation1]
}

@http(uri: "/1", method: "GET")
operation Operation1 {}
