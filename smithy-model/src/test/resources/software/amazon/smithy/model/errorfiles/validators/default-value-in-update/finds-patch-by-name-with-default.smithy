$version: "2.0"

namespace smithy.example

operation PatchFoo {
    input := {
        @required
        id: String

        @default
        description: String
    }
    output := {}
}
