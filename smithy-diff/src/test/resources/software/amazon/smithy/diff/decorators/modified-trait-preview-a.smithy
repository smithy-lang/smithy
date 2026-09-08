$version: "2.0"

namespace smithy.example

// A preview operation whose input member carries a custom trait that uses the legacy ModifiedTrait diff tags
// (diff.error.update). Changing the trait value emits a ModifiedTrait.Update ERROR on the member, which is in
// the preview closure and is therefore expected to be downgraded to WARNING.
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    operations: [
        PreviewOp
    ]
}

@unstable(featureId: "EXAMPLE_PREVIEW")
operation PreviewOp {
    input: PreviewIn
}

structure PreviewIn {
    @customConst("v1")
    data: String
}

@trait(selector: "*")
@tags(["diff.error.update"])
string customConst
