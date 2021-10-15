$version: "1.0"

metadata validators = [
    {
        name: "EmitEachSelector",
        id: "Test",
        severity: "WARNING",
        configuration: {
            selector: ":is([id=smithy.example#Foo], [id=smithy.example#Baz])"
        }
    }
]

namespace smithy.example

@suppress(["NeverUsed"])
string NoMatches

@suppress(["NeverUsed"])
structure Foo {}

@suppress(["NeverUsed"])
structure Baz {}
