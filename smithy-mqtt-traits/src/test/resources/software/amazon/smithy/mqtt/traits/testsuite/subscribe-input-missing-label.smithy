// mqttSubscribe operations do not support input. This test
// should detect that members of the input of the operation are
// missing mqttTopicLabel traits.

namespace smithy.example

@mqttSubscribe("events/{foo}")
@outputEventStream("messages")
operation Foo(FooInput) -> FooOutput

structure FooInput {
  @required
  @mqttTopicLabel
  foo: smithy.api#String,

  baz: smithy.api#String, // Error, missing topicLabel.
}

structure FooOutput {
  messages: Event1,
}

structure Event1 {}
