$version: "1.0"

namespace smithy.example

@trait(breakingChanges: [{change: "presence", path: "/foo/bar"}])
structure exampleTrait {
    foo: Nested
}

@private
structure Nested {
    bar: String
}

@exampleTrait(foo: {bar: "hi"})
string Example
