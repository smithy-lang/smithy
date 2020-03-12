namespace smithy.example

service FooService {
    version: "2020-03-12",
    operations: [Publish]
}

@smithy.mqtt#publish("foo")
operation Publish {
    input: PublishInput
}

structure PublishInput {
  @eventStream // invalid
  messages: Event,
}

structure Event {}
