$version: "2.0"

namespace smithy.example

// ChangedMemberOrder on a structure inside an operation-level preview owner. The members are swapped.
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    operations: [
        PreviewOp
    ]
}

@unstable(featureId: "EXAMPLE_PREVIEW")
operation PreviewOp {
    input: PreviewIn
}

structure PreviewIn {
    b: String
    a: String
}
