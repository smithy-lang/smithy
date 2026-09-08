$version: "2.0"

namespace smithy.example

// AddedRequiredMember where the new member is itself the preview owner, on a GA operation's input. This makes
// the GA structure gain a non-nullable member, so it stays blocking like any other nullability change.
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
    data: String
}
