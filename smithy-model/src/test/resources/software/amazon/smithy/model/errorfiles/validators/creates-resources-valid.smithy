$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
    { id: "MemberShouldReferenceResource", namespace: "com.example" }
]

namespace com.example

resource Foo {
    identifiers: { fooId: String }
}

resource Bar {
    identifiers: { barId: String }
}

@createsResources([
    {
        resource: "com.example#Foo"
        identifiers: { fooId: { path: "output.fooId" } }
    }
    {
        resource: "com.example#Bar"
        identifiers: { barId: { path: "output.bars[*].barId" } }
    }
])
operation CreateFooAndBar {
    input: CreateFooAndBarInput
    output: CreateFooAndBarOutput
}

@input
structure CreateFooAndBarInput {}

structure CreateFooAndBarOutput {
    output: OutputData
}

structure OutputData {
    fooId: String
    bars: BarList
}

list BarList {
    member: BarData
}

structure BarData {
    barId: String
}
