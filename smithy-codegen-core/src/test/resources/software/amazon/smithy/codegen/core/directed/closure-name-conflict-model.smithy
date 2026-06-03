$version: "2.0"

metadata shapeClosures = [
    // Spans two namespaces that each define a shape whose name differs only by case,
    // which is allowed in a shape closure but rejected by some code generators.
    {
        id: "smithy.example#conflicting"
        includeNamespaces: ["smithy.example", "smithy.other"]
    }
    // The same closure, but the conflict is disambiguated with a rename.
    {
        id: "smithy.example#disambiguated"
        includeNamespaces: ["smithy.example", "smithy.other"]
        rename: {
            "smithy.other#city": "OtherCity"
        }
    }
]

namespace smithy.example

structure City {
    name: String
}
