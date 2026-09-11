$version: "2.0"

namespace smithy.example

// ChangedMemberTarget inside a service-level preview owner's closure. Per the SEP the whole service closure is
// downgraded.
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
    data: String
}
