$version: "2.0"

namespace smithy.example

// The feature entry has no `reason`, so it is not a PREVIEW feature and nothing it governs is downgraded.
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
    data: String
}
