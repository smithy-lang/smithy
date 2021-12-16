$version: "2.0"

namespace smithy.example

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restJson1

@title("Example")
@service(sdkId: "Example")
@sigv4(name: "Example")
@restJson1
service Example {
    version: "2020-09-11",
    operations: [GetItem],
}

@http(uri: "/", method: "GET")
@readonly
operation GetItem {
    input: GetItemRequest,
    output: GetItemResponse,
}

structure GetItemRequest {}

structure GetItemResponse {
    @httpPayload
    item: ItemResponse
}

union ItemResponse {
    Foo: Foo,
}

structure Foo {}
