$version: "2.0"

namespace smithy.example

// Dedicated input and output are created
// and the old shared shape removed
operation GetFoo {
    input: GetFooInput
    output: GetFooOutput
}

@input
structure GetFooInput {}

@output
structure GetFooOutput {}
