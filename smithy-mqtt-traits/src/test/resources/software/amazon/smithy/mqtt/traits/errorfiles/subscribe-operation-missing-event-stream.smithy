// @smithy.mqtt#subscribe operations must have an event stream in its output.

namespace smithy.example

service FooService {
    version: "2020-03-12",
    operations: [Foo]
}

@smithy.mqtt#subscribe("events")
operation Foo {
    output: FooOutput
}

structure FooOutput {}
