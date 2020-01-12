namespace smithy.example

@protocols([{"name": "aws.rest-json", auth: ["aws.v4"]}])
service Small {
  version: "2018-01-01",
  operations: [SmallOperation]
}

@http(method: "GET", uri: "/")
operation SmallOperation {
    input: SmallOperationInput
}

structure SmallOperationInput {}
