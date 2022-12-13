$version: "2.0"

namespace smithy.example

/// Service
@aws.protocols#restJson1
service MyDocs {
  version: "2018-01-01",
  operations: [MyDocsOperation]
}

/// Operation
@http(method: "GET", uri: "/")
@readonly
operation MyDocsOperation {
    output: Output
}

/// Output
structure Output {
    /// foo member.
    foo: String,

    /// Note: these member docs are ignored and instead only the documentation
    /// on the targeted structure is present in the output. This is because our
    /// users have told us that it's more important to reuse structure definitions
    /// than it is to have 100% fidelity with the original Smithy model. In a
    /// previous implementation, we created a unique named shape for every member,
    /// but this results in no shape reuse across the generated OpenAPI model.
    nested: Nested,
}

/// Nested
structure Nested {
    baz: String,
}
