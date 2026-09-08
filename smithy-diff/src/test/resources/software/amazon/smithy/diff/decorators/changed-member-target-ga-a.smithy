$version: "2.0"

namespace smithy.example

// Baseline control: no @unstable anywhere, so the decorator must leave the event completely alone.
service Example {
    version: "2020-01-01"
    operations: [
        GaOp
    ]
}

operation GaOp {
    input: GaIn
}

structure GaIn {
    data: String
}
