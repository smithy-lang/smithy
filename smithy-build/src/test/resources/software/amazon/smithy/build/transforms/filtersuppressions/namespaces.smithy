$version: "2.0"

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

metadata suppressions = [
    {
        id: "Test",
        reason: "reason...",
        namespace: "smithy.example"
    },
    {
        id: "Lorem",
        namespace: "smithy.foo"
    },
    {
        id: "Test",
        namespace: "smithy.example.nested",
        reason: "reason..."
    },
    {
        id: "Ipsum",
        namespace: "*"
    }
]

namespace smithy.example

structure Foo {}

structure Baz {}
