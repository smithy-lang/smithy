$version: "2.0"

namespace smithy.example

// RemovedShape where the removed member was itself the preview owner. It is absent from the new model, so the
// decorator resolves ownership from the old model.
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

    @unstable(featureId: "EXAMPLE_PREVIEW")
    goingAway: String
}
