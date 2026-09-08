$version: "2.0"

namespace smithy.example

// See trait-breaking-change-a.smithy.
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    operations: [
        GuardFeatureIdOp
        GuardBareOp
        GuardNewOp
    ]
}

// Gains a featureId it did not have, and its input member changes type in the same revision.
@unstable(featureId: "EXAMPLE_PREVIEW")
operation GuardFeatureIdOp {
    input: GuardIn
}

structure GuardIn {
    data: Integer
}

@unstable
operation GuardBareOp {}

@unstable(featureId: "EXAMPLE_PREVIEW")
operation GuardNewOp {}
