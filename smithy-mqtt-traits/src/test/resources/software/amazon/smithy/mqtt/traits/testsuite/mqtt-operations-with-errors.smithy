// MQTT publish/subscribe operations should not have errors.

namespace smithy.example

@smithy.mqtt#subscribe("event1")
operation Foo() -> FooOutput errors [Error]

structure FooOutput {
  @eventStream
  messages: Event1,
}

structure Event1 {}

@error("client")
structure Error {}


@smithy.mqtt#publish("event2")
operation Baz(BazInput) errors [Error]

structure BazInput {}
