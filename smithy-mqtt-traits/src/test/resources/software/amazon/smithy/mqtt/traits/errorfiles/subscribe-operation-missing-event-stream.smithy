// @smithy.mqtt#subscribe operations must have an event stream in its output.

namespace smithy.example

@smithy.mqtt#subscribe("events")
operation Foo {
    input: Unit,
    output: FooOutput
}

@output
structure FooOutput {}
