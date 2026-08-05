$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
]

namespace com.example

resource Foo {
    identifiers: { fooId: String }
}

@createsResources([
    {
        resource: Foo
        identifiersFrom: "@"
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
