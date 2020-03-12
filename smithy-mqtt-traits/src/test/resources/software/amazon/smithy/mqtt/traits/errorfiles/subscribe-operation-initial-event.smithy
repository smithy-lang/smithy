// Subscribe operations do not support initial events.

namespace smithy.example

service FooService {
    version: "2020-03-12",
    operations: [Foo]
}

@smithy.mqtt#subscribe("events")
operation Foo {
    output: FooOutput
}

structure FooOutput {
  badMember: smithy.api#String, // <-- Erroneous initial event member
  @eventStream
  messages: Event1,
}

structure Event1 {}
