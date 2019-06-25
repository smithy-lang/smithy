namespace smithy.example

@mqttPublish("foo")
@inputEventStream("messages") // Invalid
operation Publish(PublishInput)

structure PublishInput {
  messages: Event,
}

structure Event {}
