$version: "2.0"

namespace smithy.example

// See changed-operation-input-a.smithy. The input shape is swapped for a different one.
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    operations: [
        OpInputOp
    ]
}

@unstable(featureId: "EXAMPLE_PREVIEW")
operation OpInputOp {
    input: OpInputInB
}

structure OpInputInB {
    a: String
}
