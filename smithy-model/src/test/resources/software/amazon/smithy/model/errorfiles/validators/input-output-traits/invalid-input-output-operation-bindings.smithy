$version: "1.0"
namespace smithy.example

operation GetFoo {
    input: GetFooOutput,
    output: GetFooInput // these are flipped!
}

@input
structure GetFooInput {}

@output
structure GetFooOutput {}
