// MQTT publish/subscribe operations should not have errors.
$version: "2.0"

namespace smithy.example

@smithy.mqtt#subscribe("event1")
operation Foo {
    input: Unit,
    output: FooOutput,
    errors: [Error]
}

@output
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
    output: Unit,
    errors: [Error]
}

@input
structure BazInput {}
