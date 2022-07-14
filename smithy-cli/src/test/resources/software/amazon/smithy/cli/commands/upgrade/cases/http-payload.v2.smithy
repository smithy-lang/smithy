$version: "2.0"

namespace smithy.example

structure RequiredPayload {
    @required
    payload: StreamingBlob
}

structure NeitherRequiredNorDefaultPayload {
    @default("")
    payload: StreamingBlob
}

@streaming
blob StreamingBlob
