$version: "2.0"

namespace smithy.example

@aws.protocols#restJson1
service Greedy {
  version: "2018-01-01",
  operations: [GreedyOperation]
}

@http(method: "GET", uri: "/{greedy+}")
operation GreedyOperation {
    input: GreedyOperationInput
}

structure GreedyOperationInput {
    @required
    @httpLabel
    greedy: String,
}
