// Subscribe operations do not support initial events.

namespace smithy.example

@smithy.mqtt#subscribe("events")
operation Foo() -> FooOutput

structure FooOutput {
  badMember: smithy.api#String, // <-- Erroneous initial event member
  @eventStream
  messages: Event1,
}

structure Event1 {}
