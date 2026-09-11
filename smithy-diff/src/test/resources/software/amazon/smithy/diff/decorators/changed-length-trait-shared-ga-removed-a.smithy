$version: "2.0"

namespace smithy.example

// AsymStr is shared here. In -b the GA operation is dropped so the shape becomes preview-only, but customers of
// this revision still reach it through the GA operation, so tightening it stays blocking.
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    operations: [
        PreviewAsymOp
        GaAsymOp
    ]
}

@unstable(featureId: "EXAMPLE_PREVIEW")
operation PreviewAsymOp {
    input: PreviewAsymIn
}

structure PreviewAsymIn {
    data: AsymStr
}

operation GaAsymOp {
    input: GaAsymIn
}

structure GaAsymIn {
    data: AsymStr
}

@length(min: 1)
string AsymStr
