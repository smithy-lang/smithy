$version: "2"

namespace smithy.example

service InvalidExample {
    version: "2020-12-29"
    operations: [
        IgnoredBindingOperation
        NestedBindingOperation
        MixedBindingOperation
    ]
}

@http(method: "POST", uri: "/ignored-binding")
operation IgnoredBindingOperation {
    input: IgnoredBindingOperationInput
    output: IgnoredBindingOperationOutput
}

@input
structure IgnoredBindingOperationInput {
    @httpResponseCode
    code: Integer

    nestedBindings: NestedBindings
    valid: String
}

@output
structure IgnoredBindingOperationOutput {
    @httpLabel
    @required
    string: String

    @httpQuery("query")
    string2: String

    @httpQueryParams
    stringMap: StringMap

    nestedBindings: NestedBindings
    valid: String
}

@http(method: "POST", uri: "/nested-bindings")
operation NestedBindingOperation {
    input: NestedBindings
    output: NestedBindings
}

structure NestedBindings {
    @httpLabel
    @required
    string: String

    @httpQuery("query")
    string2: String

    @httpQueryParams
    stringMap: StringMap

    @httpHeader("header")
    string3: String

    @httpPrefixHeaders("x-headers-")
    stringMap2: StringMap

    @httpResponseCode
    code: Integer

    @httpPayload
    payload: Blob

    valid: String
}

map StringMap {
    key: String
    value: String
}

@http(method: "POST", uri: "/mixed-binding")
operation MixedBindingOperation {
    input: MixedBindingOperationInput
    output: MixedBindingOperationOutput
}

structure MixedBindingOperationInput with [MixedQuery, MixedResponseCode] {
    content: MixedBindingOperationOutput
}

structure MixedBindingOperationOutput with [MixedQuery, MixedResponseCode] {}

@mixin
structure MixedQuery {
    @httpQuery("query")
    query: String
}

@mixin
structure MixedResponseCode {
    @httpResponseCode
    code: Integer
}
