$version: "2.0"

namespace smithy.example

// See added-required-member-preview-member-a.smithy. A required preview member is added.
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
    data: String

    @required
    @unstable(featureId: "EXAMPLE_PREVIEW")
    addedRequiredPreviewMember: String
}
