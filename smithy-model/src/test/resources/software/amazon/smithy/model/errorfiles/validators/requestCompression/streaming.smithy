$version: "2.0"

namespace com.foo

@requestCompression(
    encodings: ["gzip"]
)
operation RequestCompressionOperation {
    input := {
        @required
        streamingMember: StreamingBlob
        member: String
    }
}

@streaming
blob StreamingBlob
