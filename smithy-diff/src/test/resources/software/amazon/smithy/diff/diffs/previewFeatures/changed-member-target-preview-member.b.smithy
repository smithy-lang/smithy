$version: "2.0"

namespace smithy.example

// See changed-member-target-preview-member-a.smithy. The target changes String -> Integer.
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
    targetChange: Integer
}
