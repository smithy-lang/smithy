$version: "2"

metadata suppressions = [
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
        identifiers: { fooId: { path: "result.fooId" } }
        identifiersFrom: "fooResult"
    }
    {
        resource: "com.example#Bar"
        identifiers: { barId: { path: "result.barId" } }
    }
])
operation CreateBoth {
    input: CreateBothInput
    output: CreateBothOutput
}

@input
structure CreateBothInput {}

structure CreateBothOutput {
    result: ResultData
    fooResult: FooResult
}

structure ResultData {
    fooId: String
    barId: String
}

structure FooResult {
    fooId: String
}

@deletesResources([
    {
        resource: "com.example#Foo"
        identifiers: { fooId: { path: "fooId" } }
    }
])
operation DeleteFoo {
    input: DeleteFooInput
    output: DeleteFooOutput
}

@input
structure DeleteFooInput {
    fooId: String
}

structure DeleteFooOutput {}
