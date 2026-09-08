$version: "2.0"

namespace smithy.example

// RemovedShape inside an operation-level preview owner. The removed member is absent from the new model, so
// the decorator resolves its ownership from the OLD model.
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    operations: [
        PreviewOp
    ]
}

@unstable(featureId: "EXAMPLE_PREVIEW")
operation PreviewOp {
    input: PreviewIn
}

structure PreviewIn {
    data: String
    gone: String
}
