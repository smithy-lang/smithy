$version: "2.0"

namespace smithy.example

use aws.apigateway#apiKeyRequired
use aws.protocols#restJson1

@restJson1
service Service {
    version: "2006-03-01"
    operations: [KeyedOperation, OpenOperation]
}

@apiKeyRequired
@http(uri: "/items", method: "GET")
operation KeyedOperation {}

@http(uri: "/health", method: "GET")
operation OpenOperation {}
