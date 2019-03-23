namespace smithy.example

// Conflicts with B, C
@mqttPublish("a")
operation A(AInput)
structure AInput {}

// Conflicts with A, C
@mqttPublish("a")
operation B(BInput)
structure BInput {}

// Conflicts with A, B
@mqttSubscribe("a")
@outputEventStream(messages)
operation C() -> COutput

structure COutput {
  messages: EmptyEvent,
}

structure EmptyEvent {}


// D and E do not conflict since they use the same payload.
@mqttPublish("b")
operation D(DInput)
structure DInput {}

@mqttPublish("b")
operation E(DInput)

@mqttSubscribe("b")
@outputEventStream(messages)
operation F() -> FOutput

structure FOutput {
  messages: DInputEvent,
}

structure DInputEvent {
  @eventPayload
  payload: DInput,
}
