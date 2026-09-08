$version: "2.0"

namespace smithy.example

// See changed-member-target-ga-a.smithy. The member target changes String -> Integer.
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
    data: Integer
}
