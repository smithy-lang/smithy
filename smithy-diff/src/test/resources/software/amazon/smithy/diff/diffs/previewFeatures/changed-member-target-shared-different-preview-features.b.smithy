$version: "2.0"

namespace smithy.example

// See changed-member-target-shared-different-preview-features-a.smithy. `targetChange` changes
// String -> Integer.
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
    targetChange: Integer
}
