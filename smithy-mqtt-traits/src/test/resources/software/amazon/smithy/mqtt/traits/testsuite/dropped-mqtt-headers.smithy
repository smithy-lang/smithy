// This test ensures that eventHeader bindings emit an error.
// Event headers are not serialized for MQTT messages.

namespace smithy.example

@mqttSubscribe("events")
@outputEventStream(messages)
operation Foo(FooInput) -> FooOutput

structure FooInput {}

structure FooOutput {
  messages: Event
}

structure Event {
  @eventHeader
  foo: smithy.api#String, // Expected error

  @eventHeader
  bar: smithy.api#String, // Expected error

  baz: smithy.api#String, // No error
}
