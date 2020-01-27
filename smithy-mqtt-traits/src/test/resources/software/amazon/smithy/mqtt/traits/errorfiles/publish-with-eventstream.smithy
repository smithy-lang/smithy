namespace smithy.example

@smithy.mqtt#publish("foo")
operation Publish {
    input: PublishInput
}

structure PublishInput {
  @eventStream // invalid
  messages: Event,
}

structure Event {}
