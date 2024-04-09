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

@requestCompression(
    encodings: ["gzip"]
)
operation RequestCompressionOperation2 {
    input := {
        @required
        member: StreamingBlob
    }
}

@streaming
blob StreamingBlob
