// AWS protocols do not currently support applying the http payload trait to
// sets, lists, or maps.
//
// This uses version 1.0 to test forbidding sets.
$version: "1.0"

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
    input: InvalidBindingOperationInput,
    output: InvalidBindingOperationOutput,
    errors: [InvalidBindingError],
}

@input
structure InvalidBindingOperationInput {
    @httpPayload
    listBinding: StringList,
}

@output
structure InvalidBindingOperationOutput {
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
