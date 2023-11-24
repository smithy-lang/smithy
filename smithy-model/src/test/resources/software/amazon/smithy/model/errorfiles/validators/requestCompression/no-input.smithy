$version: "2.0"

namespace com.foo

@requestCompression(
    encodings: ["gzip"]
)
operation RequestCompressionOperation {}
