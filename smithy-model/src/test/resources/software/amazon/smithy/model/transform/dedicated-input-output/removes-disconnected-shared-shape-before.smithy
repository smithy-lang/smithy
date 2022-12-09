$version: "2.0"

namespace smithy.example

// A shape is shared by both input and output
// but is not named properly for an output
operation GetFoo {
    input: MyGetFooOutput
    output: MyGetFooOutput
}

structure MyGetFooOutput {}
