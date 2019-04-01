namespace smithy.example

@protocols([{"name": "aws.rest-json"}])
service Greedy {
  version: "2018-01-01",
  operations: [GreedyOperation]
}

@http(method: GET, uri: "/{greedy+}")
operation GreedyOperation(GreedyOperationInput)

structure GreedyOperationInput {
    @required
    @httpLabel
    greedy: String,
}
