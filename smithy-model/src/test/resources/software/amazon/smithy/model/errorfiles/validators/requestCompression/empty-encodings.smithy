$version: "2.0"

namespace com.foo

@requestCompression(
    encodings: []
)
operation RequestCompressionOperation {
    input := {
        @required
        member: StreamingBlob
    }
}

@streaming
blob StreamingBlob
