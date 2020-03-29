namespace smithy.example

@aws.protocols#restJson1
service Streaming {
  version: "2018-01-01",
  operations: [StreamingOperation]
}

@http(method: "GET", uri: "/")
@readonly
operation StreamingOperation {
    output: Output
}

structure Output {
  @httpPayload
  @streaming
  body: StreamingPayload,
}

blob StreamingPayload
