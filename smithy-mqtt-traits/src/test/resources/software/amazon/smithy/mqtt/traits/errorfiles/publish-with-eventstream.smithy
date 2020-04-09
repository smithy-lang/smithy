namespace smithy.example

@smithy.mqtt#publish("foo")
operation Publish {
    input: PublishInput
}

structure PublishInput {
  messages: EventStream,
}

@streaming
union EventStream {
    singleton: Event
}

structure Event {}
