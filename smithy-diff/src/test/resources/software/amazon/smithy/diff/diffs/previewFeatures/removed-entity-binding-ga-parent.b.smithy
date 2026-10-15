$version: "2.0"

namespace smithy.example

// See removed-entity-binding-ga-parent-a.smithy. Every child above is dropped; GaHostResource stays but no
// longer binds PreviewBoundOp.
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    resources: [
        GaHostResource
    ]
}

resource GaHostResource {
    identifiers: {
        id: String
    }
}
