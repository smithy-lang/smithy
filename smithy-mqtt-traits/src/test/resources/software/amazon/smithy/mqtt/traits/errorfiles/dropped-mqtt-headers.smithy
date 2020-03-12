// This test ensures that eventHeader bindings emit an error.
// Event headers are not serialized for MQTT messages.

namespace smithy.example

service FooService {
    version: "2020-03-12",
    operations: [Foo]
}

@smithy.mqtt#subscribe("events")
operation Foo {
    input: FooInput,
    output: FooOutput
}

structure FooInput {}

structure FooOutput {
  @eventStream
  messages: Event
}

structure Event {
  @eventHeader
  foo: smithy.api#String, // Expected error

  @eventHeader
  bar: smithy.api#String, // Expected error

  baz: smithy.api#String, // No error
}
