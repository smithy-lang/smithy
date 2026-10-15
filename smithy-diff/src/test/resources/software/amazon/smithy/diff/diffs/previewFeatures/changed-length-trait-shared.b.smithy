$version: "2.0"

namespace smithy.example

// See changed-length-trait-shared-a.smithy. The min is tightened while the sharing remains.
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
    input: PreviewSharedIn
}

structure PreviewSharedIn {
    data: SharedStr
}

operation GaSharedOp {
    input: GaSharedIn
}

structure GaSharedIn {
    data: SharedStr
}

@length(min: 5)
string SharedStr
