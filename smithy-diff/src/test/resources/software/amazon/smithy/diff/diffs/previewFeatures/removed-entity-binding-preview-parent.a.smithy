$version: "2.0"

namespace smithy.example

// Losing a binding is reported on the PARENT. Here the parent is itself a preview service, so dropping one of
// its operations is downgraded.
@unstableFeatures(
    PREVIEWSVC: { message: "preview service", reason: "PREVIEW" }
)
@unstable(featureId: "PREVIEWSVC")
service PreviewSvc {
    version: "2020-01-01"
    operations: [
        SvcKeepOp
        SvcGoneOp
    ]
}

operation SvcKeepOp {}

operation SvcGoneOp {}
