$version: "1.0"

namespace smithy.example

structure RequiredPayload {
    @required
    payload: StreamingBlob
}

structure NeitherRequiredNorDefaultPayload {
    payload: StreamingBlob
}

@streaming
blob StreamingBlob
