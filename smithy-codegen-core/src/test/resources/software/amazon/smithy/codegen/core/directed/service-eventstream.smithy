$version: "1.0"

namespace smithy.example

service Foo {
    operations: [GetAndSendMovements]
}

operation GetAndSendMovements {
    input: GetAndSendMovementsInput,
    output: GetAndSendMovementsOutput
}

@input
structure GetAndSendMovementsInput {
    movements: MovementEvents
}

@output
structure GetAndSendMovementsOutput {
    movements: MovementEvents
}

@streaming
union MovementEvents {
    stop: Movement,
    go: Movement
}

structure Movement {}
