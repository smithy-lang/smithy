namespace smithy.example

// Conflicts with B, C
@smithy.mqtt#publish("a")
operation A(AInput)
structure AInput {}

// Conflicts with A, C
@smithy.mqtt#publish("a")
operation B(BInput)
structure BInput {}

// Conflicts with A, B
@smithy.mqtt#subscribe("a")
@outputEventStream("messages")
operation C() -> COutput

structure COutput {
  messages: EmptyEvent,
}

structure EmptyEvent {}


// D and E do not conflict since they use the same payload.
@smithy.mqtt#publish("b")
operation D(DInput)
structure DInput {}

@smithy.mqtt#publish("b")
operation E(DInput)

@smithy.mqtt#subscribe("b")
@outputEventStream("messages")
operation F() -> FOutput

structure FOutput {
  messages: DInputEvent,
}

structure DInputEvent {
  @eventPayload
  payload: DInput,
}
