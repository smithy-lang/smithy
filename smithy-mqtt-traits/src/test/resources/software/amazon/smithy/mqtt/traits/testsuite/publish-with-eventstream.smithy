namespace smithy.example

@smithy.mqtt#publish("foo")
@inputEventStream("messages") // Invalid
operation Publish(PublishInput)

structure PublishInput {
  messages: Event,
}

structure Event {}
