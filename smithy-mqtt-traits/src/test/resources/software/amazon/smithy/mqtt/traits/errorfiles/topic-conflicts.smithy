namespace smithy.example

service FooService {
    version: "2020-03-12",
    operations: [A, B, C, D, E, F]
}

// Conflicts with B, C
@smithy.mqtt#publish("a")
operation A {
    input: AInput
}

structure AInput {}

// Conflicts with A, C
@smithy.mqtt#publish("a")
operation B {
    input: BInput
}

structure BInput {}

// Conflicts with A, B
@smithy.mqtt#subscribe("a")
operation C {
    output: COutput
}

structure COutput {
  @eventStream
  messages: EmptyEvent,
}

structure EmptyEvent {}


// D and E do not conflict since they use the same payload.
@smithy.mqtt#publish("b")
operation D {
    input: DInput
}
structure DInput {}

@smithy.mqtt#publish("b")
operation E {
    input: DInput
}

@smithy.mqtt#subscribe("b")
operation F {
    output: FOutput
}

structure FOutput {
  @eventStream
  messages: DInputEvent,
}

structure DInputEvent {
  @eventPayload
  payload: DInput,
}
