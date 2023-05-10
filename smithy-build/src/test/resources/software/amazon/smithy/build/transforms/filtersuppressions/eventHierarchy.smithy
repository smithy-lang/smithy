$version: "2.0"

metadata validators = [
    {
        name: "EmitEachSelector",
        id: "A.B",
        severity: "WARNING",
        configuration: {
            selector: ":not([id='smithy.example#NoMatches'])"
        }
    },
    {
        name: "EmitEachSelector",
        id: "B.C.D",
        severity: "WARNING",
        configuration: {
            selector: ":not([id='smithy.example#NoMatches'])"
        }
    }
]

namespace smithy.example

@suppress(["A", "B.C"])
structure Foo {
    @suppress(["A", "B.C"])
    foo: String
}
