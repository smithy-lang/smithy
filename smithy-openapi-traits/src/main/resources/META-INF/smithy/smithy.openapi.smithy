$version: "2"

namespace smithy.openapi

/// Indicates a trait shape should be converted into an [OpenAPI specification extension](https://spec.openapis.org/oas/v3.1.0#specification-extensions).
@trait(
    selector: "[trait|trait]",
    breakingChanges: [
        {change: "presence"},
        {path: "/as", change: "any"}
    ]
)
structure specificationExtension {
    /// Explicitly name the specification extension.
    /// If set must begin with `x-`, otherwise defaults to the target trait shape's ID normalized with hyphens and prepended with `x-`.
    as: SpecificationExtensionKey
}

@private
@pattern("^x-.+$")
string SpecificationExtensionKey
