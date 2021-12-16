// Finds operation names that imply they are the input/output of an
// operation, but the operation uses a different input/output shape.
$version: "2.0"

namespace smithy.example

operation GetFoo {
    input: GetFoo_Input,
    output: GetFoo_Output
}

@input
structure GetFoo_Input {}

@output
structure GetFoo_Output {}

structure GetFooRequest {}

structure GetFooResponse {}

structure GetFooInput {}

structure GetFooOutput {}
