// This test ensures that eventHeader bindings emit an error.
// Event headers are not serialized for MQTT messages.
$version: "2.0"

namespace smithy.example

@smithy.mqtt#subscribe("events")
operation Foo {
    input: Unit,
    output: FooOutput
}

@output
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
