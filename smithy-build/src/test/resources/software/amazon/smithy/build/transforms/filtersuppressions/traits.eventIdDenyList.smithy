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

@suppress(["NeverUsed"])
string NoMatches

@suppress(["NeverUsed"])
structure Foo {
    @suppress(["NeverUsed"])
    foo: String
}

@suppress(["NeverUsed"])
structure Baz {
    @suppress(["NeverUsed"])
    baz:String
}
