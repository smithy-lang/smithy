$version: "2.0"

namespace smithy.example

// ChangedMemberTarget on a member-level preview owner. This is not a nullability change, so only the callers
// opting into the preview field are affected and it is downgraded.
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    operations: [
        GaOp
    ]
}

operation GaOp {
    input: GaIn
}

structure GaIn {
    @unstable(featureId: "EXAMPLE_PREVIEW")
    targetChange: String
}
