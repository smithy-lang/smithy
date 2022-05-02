$version: "1.0"

namespace smithy.example

structure RequiredPayload {
    @required
    payload: StreamingBlob
}

structure DefaultPayload {
    @default
    payload: StreamingBlob
}

structure NeitherRequiredNorDefaultPayload {
    payload: StreamingBlob
}

@streaming
blob StreamingBlob
