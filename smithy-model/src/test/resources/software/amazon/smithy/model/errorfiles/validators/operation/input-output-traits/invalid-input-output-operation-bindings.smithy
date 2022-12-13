$version: "2.0"
namespace smithy.example

operation GetFoo {
    input: GetFooOutput,
    output: GetFooInput // these are flipped!
}

@input
@suppress(["OperationNameAmbiguity"])
structure GetFooInput {}

@output
@suppress(["OperationNameAmbiguity"])
structure GetFooOutput {}
