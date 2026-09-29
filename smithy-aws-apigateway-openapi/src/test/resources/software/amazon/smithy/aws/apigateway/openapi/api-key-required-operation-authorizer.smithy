$version: "2.0"

// Scenarios A4, A6, A7, B1, B2, B3 and B4: @apiKeyRequired on a service that declares authorizers but
// no service-level @authorizer. The service supports three auth schemes, which serves two purposes:
// operations can narrow their auth with @auth to one (A4, B3) or two (A6, B4) of them and thereby
// make the core converter emit operation-level requirements, the only path that exercises the
// updateSecurity hooks, and an operation inheriting all three shows them restated as separate
// alternatives (A7).

namespace smithy.example

use aws.apigateway#apiKeyRequired
use aws.apigateway#authorizer
use aws.apigateway#authorizers
use aws.auth#sigv4
use aws.protocols#restJson1

@restJson1
@sigv4(name: "example")
@httpBasicAuth
@httpBearerAuth
@authorizers(
    tokenAuth: { scheme: "smithy.api#httpBearerAuth", type: "token", uri: "arn:token", customAuthType: "custom" }
)
service OperationAuthorizerService {
    version: "2006-03-01"
    operations: [
        NarrowedAuthWithKey
        TwoSchemesWithKey
        InheritsAllSchemesWithKey
        AuthorizerNoKey
        AuthorizerWithKey
        NarrowedAuthAndAuthorizerWithKey
        TwoSchemesAndAuthorizerWithKey
    ]
}

// A4: narrows auth to one of the service's three schemes and requires a key, with no authorizer. The
// core converter emits a requirement for the narrowed scheme and the key is merged into it.
@auth([sigv4])
@apiKeyRequired
@http(uri: "/a4-narrowed-auth-with-key", method: "GET")
operation NarrowedAuthWithKey {}

// A6: narrows auth to two of the service's three schemes and requires a key. Both requirements are
// emitted, the key is merged into each, and the schemes remain separate alternatives.
@auth([httpBasicAuth, httpBearerAuth])
@apiKeyRequired
@http(uri: "/a6-two-schemes-with-key", method: "GET")
operation TwoSchemesWithKey {}

// A7: inherits ALL of the service's auth schemes and requires a key. The schemes are alternatives,
// so each must become its own requirement with the key added, rather than being combined into one
// requirement demanding all schemes at once.
@apiKeyRequired
@http(uri: "/a7-inherits-all-schemes-with-key", method: "GET")
operation InheritsAllSchemesWithKey {}

// B1 (control): names an authorizer and requires no key.
@authorizer("tokenAuth")
@http(uri: "/b1-authorizer-no-key", method: "GET")
operation AuthorizerNoKey {}

// B2: names an authorizer and requires a key. The operation's auth schemes match the service's, so
// the core converter emits nothing and the requirement is written from the resolved authorizer.
@authorizer("tokenAuth")
@apiKeyRequired
@http(uri: "/b2-authorizer-with-key", method: "GET")
operation AuthorizerWithKey {}

// B3: narrows auth AND names an authorizer AND requires a key. Here (and in B4) the relative order
// of AddAuthorizers and AddApiKeyRequired is observable: AddAuthorizers renames the emitted scheme
// key to the authorizer name and only does so for a single-entry requirement, so it must run before
// the key is merged in. The narrowed scheme matches the one the authorizer
// references, so the rename relabels the same mechanism rather than replacing it.
@auth([httpBearerAuth])
@authorizer("tokenAuth")
@apiKeyRequired
@http(uri: "/b3-narrowed-auth-and-authorizer-with-key", method: "GET")
operation NarrowedAuthAndAuthorizerWithKey {}

// B4: two narrowed schemes plus an authorizer. Each emitted single-entry requirement is renamed to
// the resolved authorizer, so both become {tokenAuth}, the key is merged into each, and the two
// identical requirements collapse into one: the authorizer flattens the alternatives.
@auth([httpBasicAuth, httpBearerAuth])
@authorizer("tokenAuth")
@apiKeyRequired
@http(uri: "/b4-two-schemes-and-authorizer-with-key", method: "GET")
operation TwoSchemesAndAuthorizerWithKey {}
