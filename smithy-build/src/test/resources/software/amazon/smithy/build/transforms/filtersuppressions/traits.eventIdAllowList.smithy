$version: "2.0"

metadata validators = [
    {
        name: "EmitEachSelector",
        id: "Test",
        severity: "WARNING",
        configuration: {
            selector: ":not([id='smithy.example#NoMatches'])"
        }
    }
]

namespace smithy.example

@suppress(["Test"])
string NoMatches

@suppress(["Test"])
structure Foo {
    @suppress(["Test"])
    foo: String
}

structure Baz {
    baz:String
}
