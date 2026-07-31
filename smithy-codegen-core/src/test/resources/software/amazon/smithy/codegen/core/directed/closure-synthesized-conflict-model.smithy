$version: "2.0"

metadata shapeClosures = [
    // Spans two namespaces. smithy.other#makeThingOutput conflicts case-insensitively
    // with the smithy.example#MakeThingOutput shape that createDedicatedInputsAndOutputs
    // synthesizes for MakeThing. The conflict therefore only exists *after* transforms.
    {
        id: "smithy.example#synthesizedConflict"
        includeNamespaces: ["smithy.example", "smithy.other"]
    }
]

namespace smithy.example

// Uses a shared, non-dedicated output shape so that createDedicatedInputsAndOutputs
// must synthesize a MakeThingOutput shape.
operation MakeThing {
    output: SharedOutput
}

structure SharedOutput {
    name: String
}
