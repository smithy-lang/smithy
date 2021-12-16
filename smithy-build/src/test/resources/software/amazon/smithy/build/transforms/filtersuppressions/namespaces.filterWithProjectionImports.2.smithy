$version: "2.0"

metadata validators = [
    {
        name: "EmitEachSelector",
        id: "Test2",
        severity: "WARNING",
        configuration: {
            selector: ":is([id|name^=Foo])"
        }
    }
]

namespace smithy.example

@suppress(["Test"]) // unused
structure Foo2 {}
