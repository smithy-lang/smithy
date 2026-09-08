$version: "2.0"

namespace smithy.example

// SharedIn is the input of both a preview operation and a GA operation. In -b its `extra` member loses
// @required (a nullability change) and a new @required `data` member is added. Because the GA operation also
// exposes the structure, both changes affect that operation's existing customers, so both stay blocking even
// though a preview operation also uses it.
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    operations: [
        PreviewSharedOp
        GaSharedOp
    ]
}

@unstable(featureId: "EXAMPLE_PREVIEW")
operation PreviewSharedOp {
    input: SharedIn
}

operation GaSharedOp {
    input: SharedIn
}

structure SharedIn {
    @required
    extra: String
}
