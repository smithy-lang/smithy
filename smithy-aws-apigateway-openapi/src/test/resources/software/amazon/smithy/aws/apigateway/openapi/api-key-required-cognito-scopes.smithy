$version: "2.0"

// Scenarios E1 through E5: @apiKeyRequired combined with Cognito User Pools scopes. Unlike the
// other scenarios, the scoped requirement is written by AddCognitoUserPoolsScopes#updateOperation
// (default order 0) rather than by the core converter, so AddApiKeyRequired#updateOperation
// (order 1) must merge api_key into a requirement another mapper added.

namespace smithy.example

use aws.apigateway#apiKeyRequired
use aws.auth#cognitoUserPools
use aws.auth#cognitoUserPoolsScopes
use aws.protocols#restJson1

@restJson1
@cognitoUserPools(
    providerArns: ["arn:aws:cognito-idp:us-east-1:123456789012:userpool/us-east-1_abc123"]
)
service CognitoScopesService {
    version: "2006-03-01"
    operations: [DisabledAuthNoKey, ScopedNoKey, ScopedWithKey, UnscopedWithKey, DisabledAuthWithKey]
}

// E1 (control): removes the service's Cognito auth with @auth([]) and requires no key. Even though
// scopes are declared, Cognito is not an effective auth scheme for the operation, so no mapper
// writes a requirement and the explicitly empty security list stands.
@auth([])
@cognitoUserPoolsScopes(["email", "profile"])
@http(uri: "/e1-disabled-auth-no-key", method: "GET")
operation DisabledAuthNoKey {}

// E2 (control): scopes without a key.
@cognitoUserPoolsScopes(["email", "profile"])
@http(uri: "/e2-scoped-no-key", method: "GET")
operation ScopedNoKey {}

// E3: scopes and a key. The key must be merged into the scoped requirement rather than appended as
// a separate (key-only) alternative, and the scoped requirement must not make the key disappear.
@cognitoUserPoolsScopes(["email", "profile"])
@apiKeyRequired
@http(uri: "/e3-scoped-with-key", method: "GET")
operation ScopedWithKey {}

// E4: a key without scopes. The operation inherits the service's Cognito auth, which is restated
// with the key merged in.
@apiKeyRequired
@http(uri: "/e4-unscoped-with-key", method: "GET")
operation UnscopedWithKey {}

// E5: removes the service's Cognito auth with @auth([]) and requires a key, with no scopes. There
// is no auth to preserve and no scoped requirement to merge into, so the key stands alone.
@auth([])
@apiKeyRequired
@http(uri: "/e5-disabled-auth-with-key", method: "GET")
operation DisabledAuthWithKey {}
