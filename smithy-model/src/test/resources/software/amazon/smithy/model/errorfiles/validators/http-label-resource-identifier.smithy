$version: "2.0"

namespace smithy.example

resource Foo {
    identifiers: { fooId: String }
    resources: [Bar]
}

resource Bar {
    identifiers: {
        fooId: String
        barId: String
    }
    read: GetBar
}

@readonly
@http(method: "GET", uri: "/foo/{fooId}/bar/{barId}")
operation GetBar {
    input := {
        @httpLabel
        @required
        @resourceIdentifier("fooId")
        id: String

        @httpLabel
        @required
        barId: String
    }
}
