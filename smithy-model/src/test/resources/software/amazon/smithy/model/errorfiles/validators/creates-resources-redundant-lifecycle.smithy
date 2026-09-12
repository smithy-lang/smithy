$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
]

namespace com.example

resource Foo {
    identifiers: { fooId: String }
    create: CreateFoo
}

@createsResources([
    {
        resource: "com.example#Foo"
        identifiers: { fooId: { path: "fooId" } }
    }
])
operation CreateFoo {
    input: CreateFooInput
    output: CreateFooOutput
}

@input
structure CreateFooInput {}

structure CreateFooOutput {
    fooId: String
}
