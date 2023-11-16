$version: "2.0"

namespace smithy.example

operation A {
    input: AInput,
    output: AOutput
}

@input
structure AInput {
    foo: String,
}

@output
structure AOutput {
    baz: String,
}
