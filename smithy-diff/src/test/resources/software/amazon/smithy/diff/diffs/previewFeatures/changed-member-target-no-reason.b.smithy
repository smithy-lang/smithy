$version: "2.0"

namespace smithy.example

// See changed-member-target-no-reason-a.smithy. The member target changes String -> Integer.
@unstableFeatures(
    EXAMPLE_NO_REASON: { message: "no reason, so not a preview feature" }
)
service Example {
    version: "2020-01-01"
    operations: [
        NoReasonOp
    ]
}

@unstable(featureId: "EXAMPLE_NO_REASON")
operation NoReasonOp {
    input: NoReasonIn
}

structure NoReasonIn {
    data: Integer
}
