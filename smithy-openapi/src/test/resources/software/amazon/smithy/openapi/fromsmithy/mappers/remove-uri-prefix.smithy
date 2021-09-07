$version: "1.0"

namespace smithy.example

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restJson1

@sigv4(name: "Example")
@restJson1
service Example {
    version: "2020-09-11",
    operations: [GetItem, PutItem],
}

@http(uri: "/v1/item/more-nesting", method: "GET")
@readonly
operation GetItem {
    input: GetItemRequest,
    output: GetItemResponse,
}

structure GetItemRequest {}

structure GetItemResponse {}

@http(uri: "/v1/item/more-nesting", method: "PUT")
@idempotent
operation PutItem {
    input: PutItemRequest
}
structure PutItemRequest {}
