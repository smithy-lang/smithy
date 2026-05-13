$version: "2.0"

namespace smithy.example

use aws.auth#cognitoUserPools
use aws.auth#cognitoUserPoolsScopes
use aws.protocols#restJson1

@restJson1
@cognitoUserPools(
    providerArns: ["arn:aws:cognito-idp:us-east-1:123456789012:userpool/us-east-1_abc123"]
)
service Service {
    version: "2006-03-01"
    operations: [ScopedOperation, UnscopedOperation, NoAuthOperation]
}

@cognitoUserPoolsScopes(["email", "profile"])
@http(uri: "/scoped", method: "GET")
operation ScopedOperation {}

@http(uri: "/unscoped", method: "GET")
operation UnscopedOperation {}

/// Explicitly opts out of authentication. Even though scopes are set,
/// the mapper must respect @auth([]) and not add a security requirement.
@auth([])
@cognitoUserPoolsScopes(["email"])
@http(uri: "/noauth", method: "GET")
operation NoAuthOperation {}
