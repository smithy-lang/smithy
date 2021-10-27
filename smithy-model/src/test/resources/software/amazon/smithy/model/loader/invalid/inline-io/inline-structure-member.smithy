// Defining structures inline with the `:=` syntax may only be used when defining operation input and output shapes.
$version: "2.0"
namespace smithy.example

structure Structure {
    inlined := String
}
