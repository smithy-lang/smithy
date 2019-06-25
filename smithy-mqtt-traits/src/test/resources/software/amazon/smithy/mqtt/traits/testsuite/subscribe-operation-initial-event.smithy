// Subscribe operations do not support initial events.

namespace smithy.example

@mqttSubscribe("events")
@outputEventStream("messages")
operation Foo() -> FooOutput

structure FooOutput {
  badMember: smithy.api#String, // <-- Erroneous initial event member
  messages: Event1,
}

structure Event1 {}
