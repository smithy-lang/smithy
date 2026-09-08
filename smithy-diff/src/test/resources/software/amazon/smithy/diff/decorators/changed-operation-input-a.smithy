$version: "2.0"

namespace smithy.example

// ChangedOperationInput is reported on the operation itself.
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
    input: OpInputInA
}

structure OpInputInA {
    a: String
}
