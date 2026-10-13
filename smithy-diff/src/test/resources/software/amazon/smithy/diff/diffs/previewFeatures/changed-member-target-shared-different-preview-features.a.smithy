$version: "2.0"

namespace smithy.example

// ChangedMemberTarget on a member of a structure shared by two operations that are preview owners of
// DIFFERENT preview features (EXAMPLE_PREVIEW and EXAMPLE_PREVIEW_TWO). No GA operation exposes InputStruct,
// so every path that reaches it is a preview opt-in and the change is downgraded.
@unstableFeatures(
    EXAMPLE_PREVIEW: { message: "preview", reason: "PREVIEW" }
    EXAMPLE_PREVIEW_TWO: { message: "preview2", reason: "PREVIEW" }
)
service Example {
    version: "2020-01-01"
    operations: [
        OpOne
        OpTwo
    ]
}

@unstable(featureId: "EXAMPLE_PREVIEW_TWO")
operation OpOne {
    input: InputStruct
}

@unstable(featureId: "EXAMPLE_PREVIEW")
operation OpTwo {
    input: InputStruct
}

structure InputStruct {
    targetChange: String
}
