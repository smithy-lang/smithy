$version: "1.0"

namespace smithy.example

@trait(breakingChanges: [{change: "update", path: "/foo"}])
union exampleTrait {
    foo: String
}

@exampleTrait(foo: "hi")
string Example
