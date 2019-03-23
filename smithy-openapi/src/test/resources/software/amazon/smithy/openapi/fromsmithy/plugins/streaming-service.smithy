namespace smithy.example

@protocols(aws.rest-json: {})
service Streaming {
  version: "2018-01-01",
  operations: [StreamingOperation]
}

@http(method: GET, uri: "/")
operation StreamingOperation(Input)

structure Input {
  @httpPayload
  body: StreamingPayload,
}

@streaming
blob StreamingPayload
