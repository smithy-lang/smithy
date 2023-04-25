$version: "2.0"

namespace com.foo

@requestCompression(
    encodings: ["gzip"]
)
operation RequestCompressionOperation {
    input := {
        @required
        member: StreamingBlob
    }
}

@streaming
@requiresLength
blob StreamingBlob
