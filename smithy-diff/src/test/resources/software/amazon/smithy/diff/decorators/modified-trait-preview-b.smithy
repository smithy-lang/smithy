$version: "2.0"

namespace smithy.example

// See modified-trait-preview-a.smithy. The customConst trait value on `data` changes v1 -> v2.
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
    @customConst("v2")
    data: String
}

@trait(selector: "*")
@tags(["diff.error.update"])
string customConst
