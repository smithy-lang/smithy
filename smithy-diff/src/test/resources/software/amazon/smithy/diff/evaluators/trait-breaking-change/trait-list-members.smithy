$version: "1.0"

namespace smithy.example

@trait(breakingChanges: [{change: "update", path: "/member"}])
list exampleTrait {
    member: String
}

@exampleTrait(["a", "b", "c"])
string Example
