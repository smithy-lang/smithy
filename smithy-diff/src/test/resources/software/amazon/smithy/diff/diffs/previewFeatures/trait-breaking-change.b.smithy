$version: "2.0"

namespace smithy.example

// See trait-breaking-change-a.smithy.
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
    EXAMPLE_PREVIEW_TWO: { message: "preview2", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    operations: [
        GuardFeatureIdOp
        GuardUpdateOp
        GuardRemoveOp
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

// Changes its featureId from EXAMPLE_PREVIEW to EXAMPLE_PREVIEW_TWO.
@unstable(featureId: "EXAMPLE_PREVIEW_TWO")
operation GuardUpdateOp {}

// Drops its featureId, becoming a bare @unstable.
@unstable
operation GuardRemoveOp {}

@unstable
operation GuardBareOp {}

@unstable(featureId: "EXAMPLE_PREVIEW")
operation GuardNewOp {}
