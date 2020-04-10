// This test ensures that eventHeader bindings emit an error.
// Event headers are not serialized for MQTT messages.

namespace smithy.example

@smithy.mqtt#subscribe("events")
operation Foo {
    input: FooInput,
    output: FooOutput
}

structure FooInput {}

structure FooOutput {
  messages: EventStream
}

@streaming
union EventStream {
    singleton: Event
}

structure Event {
  @eventHeader
  foo: smithy.api#String, // Expected error

  @eventHeader
  bar: smithy.api#String, // Expected error

  baz: smithy.api#String, // No error
}
