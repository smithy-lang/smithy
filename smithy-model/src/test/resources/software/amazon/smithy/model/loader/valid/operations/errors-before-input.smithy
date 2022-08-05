$version: "2.0"
namespace smithy.example

operation GetFoo {
    errors: []
    input: GetFooInput
}

@input
structure GetFooInput {}
