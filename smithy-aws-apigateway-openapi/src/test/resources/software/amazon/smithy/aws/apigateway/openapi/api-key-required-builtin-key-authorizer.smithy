$version: "2.0"

// Scenarios D1 and D2: a service whose authorizer is API Gateway's built-in API key
// mechanism, which AddAuthorizers detects by the authorizer having no type and no customAuthType
// while its scheme is httpApiKeyAuth. That case is deliberately restated on every operation even
// when it matches the service, so it is the one authorizer configuration that reaches the key merge with the
// service and operation authorizers being equal.

namespace smithy.example

use aws.apigateway#apiKeyRequired
use aws.apigateway#authorizer
use aws.apigateway#authorizers
use aws.protocols#restJson1

@restJson1
@httpApiKeyAuth(name: "x-api-key", in: "header")
@authorizer("apiKeyAuth")
@authorizers(
    apiKeyAuth: { scheme: "smithy.api#httpApiKeyAuth" }
)
service BuiltInKeyAuthorizerService {
    version: "2006-03-01"
    operations: [BuiltInKeyNoKey, BuiltInKeyWithKey]
}

// D1 (control): the built-in key authorizer without @apiKeyRequired.
@http(uri: "/d1-built-in-key-no-key", method: "GET")
operation BuiltInKeyNoKey {}

// D2: requires a key on top of the built-in key authorizer.
@apiKeyRequired
@http(uri: "/d2-built-in-key-with-key", method: "GET")
operation BuiltInKeyWithKey {}
