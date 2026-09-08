$version: "2.0"

namespace smithy.example

// Guards around applying @unstable's featureId to an EXISTING shape. GuardFeatureIdOp gains a featureId in -b
// AND has a breaking input change, which exercises both the trait-level guard and the decorator's rule that a
// shape must resolve as preview in BOTH models to be downgraded. GuardBareOp gains a bare @unstable (allowed)
// and GuardNewOp is brand new (allowed).
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    operations: [
        GuardFeatureIdOp
        GuardBareOp
    ]
}

operation GuardFeatureIdOp {
    input: GuardIn
}

structure GuardIn {
    data: String
}

operation GuardBareOp {}
