$version: "2.0"

namespace smithy.example

// `@output` is added to `GetFooOutput`
// and a dedicated input shape is added
operation GetFoo {
    input: GetFooInput
    output: GetFooOutput
}

@input
structure GetFooInput {
}

@output
structure GetFooOutput {
}
