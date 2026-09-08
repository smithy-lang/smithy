$version: "2.0"

namespace smithy.example

// ChangedLengthTrait on a shape reachable only through an operation-level preview owner.
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
    data: LenStr
}

@length(min: 1)
string LenStr
