$version: "2.0"

namespace smithy.example

// SharedStr is reachable from both a preview and a GA operation, so it lies outside every feature and
// tightening it stays blocking.
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

@length(min: 1)
string SharedStr
