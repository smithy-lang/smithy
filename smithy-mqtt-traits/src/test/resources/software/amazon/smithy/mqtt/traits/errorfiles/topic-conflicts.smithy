$version: "2.0"

namespace smithy.example

// Conflicts with B, C
@smithy.mqtt#publish("a")
operation A {
    input: AInput,
    output: Unit
}

structure AInput {}

// Conflicts with A, C
@smithy.mqtt#publish("a")
operation B {
    input: BInput,
    output: Unit
}

structure BInput {}

// Conflicts with A, B
@smithy.mqtt#subscribe("a")
operation C {
    input: Unit,
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
    input: DInput,
    output: Unit
}

structure DInput {}

@smithy.mqtt#publish("b")
operation E {
    input: DInput,
    output: Unit
}

@smithy.mqtt#subscribe("b")
operation F {
    input: Unit,
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
