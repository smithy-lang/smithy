$version: "2.0"

namespace smithy.example

// See removed-entity-binding-preview-parent-a.smithy. SvcGoneOp is unbound.
@unstableFeatures(
    PREVIEWSVC: { message: "preview service", reason: "PREVIEW" }
)
@unstable(featureId: "PREVIEWSVC")
service PreviewSvc {
    version: "2020-01-01"
    operations: [
        SvcKeepOp
    ]
}

operation SvcKeepOp {}
