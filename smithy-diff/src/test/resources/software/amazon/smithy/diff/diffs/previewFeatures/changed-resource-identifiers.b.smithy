$version: "2.0"

namespace smithy.example

// See changed-resource-identifiers-a.smithy. The resource gains an identifier.
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    resources: [
        IdResource
    ]
}

@unstable(featureId: "EXAMPLE_PREVIEW")
resource IdResource {
    identifiers: {
        id: String
        extra: String
    }
}
