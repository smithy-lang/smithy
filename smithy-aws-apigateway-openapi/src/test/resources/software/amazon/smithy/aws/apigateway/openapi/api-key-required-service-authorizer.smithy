$version: "2.0"

// Scenarios C1, C2, C3 and C4: @apiKeyRequired on a service that applies an authorizer at the
// service level, so operations either inherit it, restate it, or replace it.

namespace smithy.example

use aws.apigateway#apiKeyRequired
use aws.apigateway#authorizer
use aws.apigateway#authorizers
use aws.protocols#restJson1

@restJson1
@httpBearerAuth
@authorizer("tokenAuth")
@authorizers(
    tokenAuth: { scheme: "smithy.api#httpBearerAuth", type: "token", uri: "arn:token", customAuthType: "custom" }
    otherAuth: { scheme: "smithy.api#httpBearerAuth", type: "token", uri: "arn:other", customAuthType: "custom" }
)
service ServiceAuthorizerService {
    version: "2006-03-01"
    operations: [
        InheritsAuthorizerNoKey
        InheritsAuthorizerWithKey
        SameAuthorizerWithKey
        DifferentAuthorizerWithKey
    ]
}

// C1 (control): inherits the service-level authorizer and requires no key, so it states no
// requirement of its own.
@http(uri: "/c1-inherits-authorizer-no-key", method: "GET")
operation InheritsAuthorizerNoKey {}

// C2: inherits the service-level authorizer and requires a key, so the inherited authorizer must be
// restated alongside the key rather than replaced by it.
@apiKeyRequired
@http(uri: "/c2-inherits-authorizer-with-key", method: "GET")
operation InheritsAuthorizerWithKey {}

// C3: restates the same authorizer the service already applies, and requires a key.
@authorizer("tokenAuth")
@apiKeyRequired
@http(uri: "/c3-same-authorizer-with-key", method: "GET")
operation SameAuthorizerWithKey {}

// C4: replaces the service-level authorizer with a different one, and requires a key.
@authorizer("otherAuth")
@apiKeyRequired
@http(uri: "/c4-different-authorizer-with-key", method: "GET")
operation DifferentAuthorizerWithKey {}
