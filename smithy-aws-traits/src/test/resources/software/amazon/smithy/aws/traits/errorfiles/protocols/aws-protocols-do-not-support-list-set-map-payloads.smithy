// AWS protocols do not currently support applying the http payload trait to
// sets, lists, or maps.

namespace smithy.example

use aws.protocols#restJson1
use smithy.api#http
use smithy.api#httpPayload

@restJson1
service InvalidExample {
    version: "2020-12-29",
    operations: [InvalidBindingOperation],
}

@http(method: "POST", uri: "/invalid-payload")
operation InvalidBindingOperation {
    input: InvalidBindingInput,
    output: InvalidBindingOutput,
    errors: [InvalidBindingError],
}

structure InvalidBindingInput {
    @httpPayload
    listBinding: StringList,
}

structure InvalidBindingOutput {
    @httpPayload
    mapBinding: StringMap,
}

@error("client")
structure InvalidBindingError {
    @httpPayload
    setBinding: StringSet
}

list StringList {
    member: String
}

set StringSet {
    member: String
}

map StringMap {
    key: String,
    value: String,
}
