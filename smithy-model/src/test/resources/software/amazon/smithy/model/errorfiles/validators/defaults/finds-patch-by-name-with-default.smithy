$version: "2.0"

namespace smithy.example

operation PatchFoo {
    input := {
        @required
        id: String

        description: String = ""
    }
    output := {}
}
