$version: "2.0"

namespace smithy.example

// See changed-nullability-shared-a.smithy. `extra` loses @required (becoming nullable) and a new @required
// `data` member is added.
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    operations: [
        PreviewSharedOp
        GaSharedOp
    ]
}

@unstable(featureId: "EXAMPLE_PREVIEW")
operation PreviewSharedOp {
    input: SharedIn
}

operation GaSharedOp {
    input: SharedIn
}

structure SharedIn {
    @required
    data: String

    extra: String
}
