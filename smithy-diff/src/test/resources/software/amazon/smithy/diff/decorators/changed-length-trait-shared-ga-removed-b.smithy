$version: "2.0"

namespace smithy.example

// See changed-length-trait-shared-ga-removed-a.smithy. GaAsymOp is gone and the min is tightened.
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    operations: [
        PreviewAsymOp
    ]
}

@unstable(featureId: "EXAMPLE_PREVIEW")
operation PreviewAsymOp {
    input: PreviewAsymIn
}

structure PreviewAsymIn {
    data: AsymStr
}

@length(min: 5)
string AsymStr
