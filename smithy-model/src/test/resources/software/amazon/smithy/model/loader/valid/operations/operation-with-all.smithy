$version: "2.0"
namespace smithy.example

operation GetFoo {
    input: GetFooInput
    output: GetFooOutput
    errors: []
}

@input
structure GetFooInput {}

@output
structure GetFooOutput {}
