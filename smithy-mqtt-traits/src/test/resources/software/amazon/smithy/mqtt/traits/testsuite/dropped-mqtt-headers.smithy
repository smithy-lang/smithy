// This test ensures that eventHeader bindings emit an error.
// Event headers are not serialized for MQTT messages.

namespace smithy.example

@smithy.mqtt#subscribe("events")
operation Foo(FooInput) -> FooOutput

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
