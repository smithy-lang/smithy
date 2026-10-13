$version: "2.0"

namespace smithy.example

// See changed-nullability-preview-member-a.smithy. The member loses @required.
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    operations: [
        GaOp
    ]
}

operation GaOp {
    input: GaIn
}

structure GaIn {
    @unstable(featureId: "EXAMPLE_PREVIEW")
    previewMember: String
}
