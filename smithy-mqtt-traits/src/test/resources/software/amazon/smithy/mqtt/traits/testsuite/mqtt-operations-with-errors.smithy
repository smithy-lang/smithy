// MQTT publish/subscribe operations should not have errors.

namespace smithy.example

@mqttSubscribe("event1")
@outputEventStream(messages)
operation Foo() -> FooOutput errors [Error]

structure FooOutput {
  messages: Event1,
}

structure Event1 {}

@error("client")
structure Error {}


@mqttPublish("event2")
operation Baz(BazInput) errors [Error]

structure BazInput {}
