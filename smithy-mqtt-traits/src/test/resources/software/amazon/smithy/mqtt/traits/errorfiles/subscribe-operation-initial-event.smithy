// Subscribe operations do not support initial events.
$version: "2.0"

namespace smithy.example

@smithy.mqtt#subscribe("events")
operation Foo {
    input: Unit,
    output: FooOutput
}

@output
structure FooOutput {
    badMember: smithy.api#String, // <-- Erroneous initial event member
    messages: EventStream,
}

@streaming
union EventStream {
    singleton: Event
}

structure Event {}
