$version: "2"

namespace smithy.benchmark.serde

resource S3Object {
    operations: [
        PutObject
        HeadObject
        CopyObject
        GetObject
    ]
}
