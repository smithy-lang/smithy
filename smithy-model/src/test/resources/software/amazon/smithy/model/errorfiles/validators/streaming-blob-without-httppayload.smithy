$version: "2.0"

namespace com.amazonaws.simple

@protocolDefinition(traits: ["smithy.api#httpPayload", "smithy.api#http", "smithy.api#streaming"])
@trait(selector: "service")
structure jsonExample {}

@jsonExample
@title("SimpleService")
service SimpleService {
    version: "2022-01-01",
    operations: [
        StreamingOperation,
    ],
}

@http(uri: "/streaming", method: "GET")
@readonly
operation StreamingOperation {
    input: StreamingOperationInput,
    output: StreamingOperationOutput,
}

@input
structure StreamingOperationInput {}

@output
structure StreamingOperationOutput {
    @required
    streamId: String,

    @default("")
    output: StreamingBlob,
}

@streaming
blob StreamingBlob
