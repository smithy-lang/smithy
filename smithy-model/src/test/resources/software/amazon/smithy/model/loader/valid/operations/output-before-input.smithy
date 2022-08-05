$version: "2.0"
namespace smithy.example

operation GetFoo {
    output: GetFooOutput
    input: GetFooInput
}

@input
structure GetFooInput {}

@output
structure GetFooOutput {}
