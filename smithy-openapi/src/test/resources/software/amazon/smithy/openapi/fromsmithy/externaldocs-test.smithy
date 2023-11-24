$version: "2.0"

namespace smithy.example

@externalDocumentation(
    "API Reference": "https://localhost/docs/service"
)
@aws.protocols#restJson1
service MyDocs {
    version: "2018-01-01",
    operations: [MyDocsOperation]
}

@externalDocumentation(
    "API Reference": "https://localhost/docs/operation"
)
@http(method: "GET", uri: "/")
@readonly
operation MyDocsOperation {
    output: Output
}

@externalDocumentation(
    "API Reference": "https://localhost/docs/output"
)
structure Output {
    foo: String
}