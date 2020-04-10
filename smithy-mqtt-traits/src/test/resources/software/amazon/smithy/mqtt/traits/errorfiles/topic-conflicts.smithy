namespace smithy.example

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
  messages: EmptyEventStream,
}

@streaming
union EmptyEventStream {
    singleton: EmptyEvent
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
  messages: DInputEventStream,
}

@streaming
union DInputEventStream {
    singleton: DInputEvent
}

structure DInputEvent {
  @eventPayload
  payload: DInput,
}
