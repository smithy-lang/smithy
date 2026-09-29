$version: "2.0"

// Scenarios A2, A3 and A5: @apiKeyRequired combined with auth inherited from the service,
// with no API Gateway authorizers involved.

namespace smithy.example

use aws.apigateway#apiKeyRequired
use aws.auth#sigv4
use aws.protocols#restJson1

@restJson1
@sigv4(name: "example")
service InheritedAuthService {
    version: "2006-03-01"
    operations: [InheritsAuthNoKey, InheritsAuthWithKey, DisabledAuthWithKey]
}

// A2 (control): inherits the service's sigv4 and requires no key, so it states no requirement of
// its own and falls back to the document-level one.
@http(uri: "/a2-inherits-auth-no-key", method: "GET")
operation InheritsAuthNoKey {}

// A3: inherits the service's sigv4 and requires a key, so both must be required together.
@apiKeyRequired
@http(uri: "/a3-inherits-auth-with-key", method: "GET")
operation InheritsAuthWithKey {}

// A5: removes authentication with @auth([]) but still requires a key, so the key stands alone.
@auth([])
@apiKeyRequired
@http(uri: "/a5-disabled-auth-with-key", method: "GET")
operation DisabledAuthWithKey {}
