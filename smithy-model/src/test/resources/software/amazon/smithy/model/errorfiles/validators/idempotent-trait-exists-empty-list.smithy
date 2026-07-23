$version: "2"

namespace smithy.example

@idempotent(exists: [])
operation CreateBucket {
    input: CreateBucketInput
    output: CreateBucketOutput
}

@input
structure CreateBucketInput {}

@output
structure CreateBucketOutput {}
