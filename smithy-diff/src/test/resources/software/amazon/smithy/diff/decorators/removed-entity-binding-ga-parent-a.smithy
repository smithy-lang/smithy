$version: "2.0"

namespace smithy.example

// Losing a binding is reported on the parent, so removing a PREVIEW child from a GA parent is still a breaking
// change to that GA parent.
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    operations: [
        PreviewGoneOp
        GaGoneOp
    ]
    resources: [
        PreviewGoneResource
        GaHostResource
    ]
}

@unstable(featureId: "EXAMPLE_PREVIEW")
operation PreviewGoneOp {
    input: PreviewGoneIn
}

structure PreviewGoneIn {
    data: String
}

operation GaGoneOp {}

@unstable(featureId: "EXAMPLE_PREVIEW")
resource PreviewGoneResource {
    identifiers: {
        id: String
    }
}

resource GaHostResource {
    identifiers: {
        id: String
    }
    operations: [
        PreviewBoundOp
    ]
}

@unstable(featureId: "EXAMPLE_PREVIEW")
operation PreviewBoundOp {
    input := {
        @required
        id: String
    }
}
