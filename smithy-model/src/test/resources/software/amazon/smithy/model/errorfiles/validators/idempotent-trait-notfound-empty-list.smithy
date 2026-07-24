$version: "2"

namespace smithy.example

@idempotent(
    notFound: []
)
operation DeleteBucket {
    input: DeleteBucketInput
    output: DeleteBucketOutput
}

@input
structure DeleteBucketInput {}

@output
structure DeleteBucketOutput {}
