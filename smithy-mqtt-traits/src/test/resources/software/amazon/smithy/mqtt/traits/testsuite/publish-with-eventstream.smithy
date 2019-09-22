namespace smithy.example

@smithy.mqtt#publish("foo")
operation Publish(PublishInput)

structure PublishInput {
  @eventStream // invalid
  messages: Event,
}

structure Event {}
