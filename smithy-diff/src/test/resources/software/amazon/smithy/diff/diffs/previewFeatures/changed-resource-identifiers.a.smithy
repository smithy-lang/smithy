$version: "2.0"

namespace smithy.example

// ChangedResourceIdentifiers is reported on the resource itself.
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
    }
}
