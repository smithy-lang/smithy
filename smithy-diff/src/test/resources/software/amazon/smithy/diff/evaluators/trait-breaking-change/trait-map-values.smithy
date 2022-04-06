$version: "1.0"

namespace smithy.example

@trait(breakingChanges: [{change: "update", path: "/value"}])
map exampleTrait {
    key: String,
    value: String
}

@exampleTrait(a: "A", b: "B")
string Example
