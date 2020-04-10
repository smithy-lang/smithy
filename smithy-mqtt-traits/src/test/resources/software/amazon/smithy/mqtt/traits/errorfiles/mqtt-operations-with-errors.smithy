// MQTT publish/subscribe operations should not have errors.

namespace smithy.example

@smithy.mqtt#subscribe("event1")
operation Foo {
    output: FooOutput,
    errors: [Error]
}

structure FooOutput {
  messages: EventStream,
}

@streaming
union EventStream {
    singleton: Event
}

structure Event {}

@error("client")
structure Error {}


@smithy.mqtt#publish("event2")
operation Baz {
    input: BazInput,
    errors: [Error]
}

structure BazInput {}
