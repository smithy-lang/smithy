$version: "2.0"

namespace smithy.example

// ChangedNullability on a member-level preview owner. The enclosing operation is GA, so making the member
// non-nullable breaks that operation's existing customers and stays blocking.
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
    @required
    @unstable(featureId: "EXAMPLE_PREVIEW")
    previewMember: String
}
