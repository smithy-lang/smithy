// @smithy.mqtt#subscribe operations must have an event stream in its output.
$version: "2.0"

namespace smithy.example

@smithy.mqtt#subscribe("events")
operation Foo {
    input: Unit,
    output: FooOutput
}

@output
structure FooOutput {}
