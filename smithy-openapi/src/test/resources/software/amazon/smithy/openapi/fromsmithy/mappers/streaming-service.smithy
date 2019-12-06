namespace smithy.example

@protocols([{"name": "aws.rest-json"}])
service Streaming {
  version: "2018-01-01",
  operations: [StreamingOperation]
}

@http(method: "GET", uri: "/")
@readonly
operation StreamingOperation() -> Output

structure Output {
  @httpPayload
  @streaming
  body: StreamingPayload,
}

blob StreamingPayload
