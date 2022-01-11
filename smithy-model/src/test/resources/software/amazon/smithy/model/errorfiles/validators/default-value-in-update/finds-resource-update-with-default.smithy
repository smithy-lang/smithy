$version: "2.0"

namespace smithy.example

resource Foo {
    identifiers: {
        id: String
    }
    update: UpdateFoo
}

operation UpdateFoo {
    input := {
        @required
        id: String

        @default
        description: String
    }
    output := {}
}
