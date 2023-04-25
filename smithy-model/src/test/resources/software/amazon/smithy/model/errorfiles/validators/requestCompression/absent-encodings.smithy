$version: "2.0"

namespace com.foo

@requestCompression
operation RequestCompressionOperation {
    input := {
        @required
        member: StreamingBlob
    }
}

@streaming
blob StreamingBlob
