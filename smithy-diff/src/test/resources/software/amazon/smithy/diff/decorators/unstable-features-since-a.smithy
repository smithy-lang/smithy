$version: "2.0"

namespace smithy.example

@unstableFeatures(
    UPDATED_PREVIEW: { reason: "PREVIEW", since: "2026-01-01" }
    REMOVED_PREVIEW: { reason: "PREVIEW", since: "2026-01-01" }
    ADDED_PREVIEW: { reason: "PREVIEW" }
    GRADUATED_PREVIEW: { reason: "PREVIEW", since: "2026-01-01" }
)
service Example {
    version: "2020-01-01"
    operations: [
        UpdatedOp
        RemovedOp
        AddedOp
        GraduatedOp
    ]
}

@unstable(featureId: "UPDATED_PREVIEW")
operation UpdatedOp {}

@unstable(featureId: "REMOVED_PREVIEW")
operation RemovedOp {}

@unstable(featureId: "ADDED_PREVIEW")
operation AddedOp {}

@unstable(featureId: "GRADUATED_PREVIEW")
operation GraduatedOp {}

// A service that is itself a preview owner.
@unstableFeatures(
    SERVICE_PREVIEW: { reason: "PREVIEW", since: "2026-01-01" }
)
@unstable(featureId: "SERVICE_PREVIEW")
service PreviewSvc {
    version: "2020-01-01"
}
