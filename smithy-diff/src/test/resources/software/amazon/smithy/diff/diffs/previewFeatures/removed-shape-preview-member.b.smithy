$version: "2.0"

namespace smithy.example

// See removed-shape-preview-member-a.smithy. The preview member is removed.
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
    keep: String
}
