$version: "2.0"

metadata validators = [
    {
        name: "EmitEachSelector",
        id: "Test",
        severity: "WARNING",
        configuration: {
            selector: ":is([id=smithy.example#Foo], [id=smithy.example#Baz])"
        }
    },
    {
        name: "EmitEachSelector",
        id: "Test2",
        severity: "WARNING",
        configuration: {
            selector: ":is([id|name^=Foo])"
        }
    }
]

metadata suppressions = [
    {
        id: "Test",
        reason: "reason...",
        namespace: "smithy.example"
    }
]

namespace smithy.example

structure Foo {}

structure Baz {}

structure Foo2 {}
