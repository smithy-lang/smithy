namespace smithy.example

@protocols([{"name": "aws.rest-json"}])
service Small {
  version: "2018-01-01",
  operations: [SmallOperation]
}

@http(method: GET, uri: "/")
operation SmallOperation(SmallOperationInput)

structure SmallOperationInput {}
