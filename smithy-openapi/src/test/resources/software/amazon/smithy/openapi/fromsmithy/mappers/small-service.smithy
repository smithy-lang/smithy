$version: "2.0"

namespace smithy.example

@aws.auth#sigv4(name: "small")
@aws.protocols#restJson1
service Small {
  version: "2018-01-01",
  operations: [SmallOperation]
}

@http(method: "POST", uri: "/")
operation SmallOperation {
    input: SmallOperationInput
}

structure SmallOperationInput {
    @default({})
    payload: StringMap
}

map StringMap {
    key: String
    value: String
}

@error("server")
@httpError(500)
structure SmallOperationException {
    message: String = null
}
