$version: "2.0"

namespace smithy.example

// Guards around the @unstable trait itself. GuardFeatureIdOp gains a featureId in -b AND has a breaking input
// change, which exercises both the trait-level guard and the decorator's rule that a shape must resolve as
// preview in BOTH models to be downgraded. GuardUpdateOp changes its featureId (EXAMPLE_PREVIEW -> _TWO) and
// GuardRemoveOp drops its featureId (-> bare @unstable); both are trait-level guards that stay blocking.
// GuardBareOp gains a bare @unstable (allowed) and GuardNewOp is brand new (allowed).
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
    ]
}

operation GuardFeatureIdOp {
    input: GuardIn
}

structure GuardIn {
    data: String
}

@unstable(featureId: "EXAMPLE_PREVIEW")
operation GuardUpdateOp {}

@unstable(featureId: "EXAMPLE_PREVIEW")
operation GuardRemoveOp {}

operation GuardBareOp {}
