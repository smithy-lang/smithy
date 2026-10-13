$version: "2.0"

namespace smithy.example

// See changed-member-target-preview-service-a.smithy. The member target changes String -> Integer.
@unstableFeatures(
    PREVIEWSVC: { message: "preview service", reason: "PREVIEW" }
)
@unstable(featureId: "PREVIEWSVC")
service PreviewSvc {
    version: "2020-01-01"
    operations: [
        SvcMemberOp
    ]
}

operation SvcMemberOp {
    input: SvcMemberIn
}

structure SvcMemberIn {
    data: Integer
}
