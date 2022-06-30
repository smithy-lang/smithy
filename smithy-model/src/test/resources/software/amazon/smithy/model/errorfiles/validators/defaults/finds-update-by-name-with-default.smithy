$version: "2.0"

namespace smithy.example

operation UpdateFoo {
    input := {
        @required
        id: String

        description: String = ""
    }
    output := {}
}
