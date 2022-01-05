$version: "1.0"

namespace smithy.example

service TestService {
    version: "1",
    operations: [Example]
}

operation Example {
    // Implicit input: Unit,
    output: ExampleOutput
}

@output
structure ExampleOutput {
    unit: Unit
}

// This shape does not conflict with the implicit Unit shape.
structure Unit {}
