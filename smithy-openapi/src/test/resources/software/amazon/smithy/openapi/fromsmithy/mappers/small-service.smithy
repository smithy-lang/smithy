$version: "2.0"

namespace smithy.example

@aws.auth#sigv4(name: "small")
@aws.protocols#restJson1
service Small {
  version: "2018-01-01",
  operations: [SmallOperation]
}

@http(method: "GET", uri: "/")
operation SmallOperation {
    input: SmallOperationInput
}

structure SmallOperationInput {}
