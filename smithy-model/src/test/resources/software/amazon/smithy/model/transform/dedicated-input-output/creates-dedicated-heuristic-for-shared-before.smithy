$version: "2.0"

namespace smithy.example

// `GetFooOutput` is shared by input and output
// and has the right naming for an output shape
operation GetFoo {
    input: GetFooOutput
    output: GetFooOutput
}

structure GetFooOutput {}
