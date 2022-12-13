$version: "2.0"

namespace smithy.example

@smithy.mqtt#publish("foo")
operation Publish {
    input: PublishInput,
    output: Unit
}

@input
structure PublishInput {
    messages: EventStream,
}

@streaming
union EventStream {
    singleton: Event
}

structure Event {}
