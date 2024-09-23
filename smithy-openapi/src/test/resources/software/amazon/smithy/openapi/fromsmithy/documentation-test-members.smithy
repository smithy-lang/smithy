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

    /// nested member.
    nested: Nested,
}

/// Nested
structure Nested {
    baz: String,
}
