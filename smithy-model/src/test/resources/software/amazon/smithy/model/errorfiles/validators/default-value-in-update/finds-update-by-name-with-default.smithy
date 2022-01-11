$version: "2.0"

namespace smithy.example

operation UpdateFoo {
    input := {
        @required
        id: String

        @default
        description: String
    }
    output := {}
}
