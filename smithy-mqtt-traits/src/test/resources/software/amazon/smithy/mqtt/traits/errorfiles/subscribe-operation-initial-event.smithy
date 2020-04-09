// Subscribe operations do not support initial events.

namespace smithy.example

@smithy.mqtt#subscribe("events")
operation Foo {
    output: FooOutput
}

structure FooOutput {
  badMember: smithy.api#String, // <-- Erroneous initial event member
  messages: EventStream,
}

@streaming
union EventStream {
    singleton: Event
}

structure Event {}
