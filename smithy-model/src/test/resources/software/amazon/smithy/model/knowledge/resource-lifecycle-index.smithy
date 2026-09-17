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
}

structure ResultData {
    fooId: String
    barId: String
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

@putsResources([
    {
        resource: "com.example#Foo"
        identifiers: { fooId: { path: "fooId" } }
    }
])
operation PutFoo {
    input: PutFooInput
    output: PutFooOutput
}

@input
structure PutFooInput {
    fooId: String
}

structure PutFooOutput {}

@readsResources([
    {
        resource: "com.example#Bar"
        identifiers: { barId: { path: "barId" } }
    }
])
operation ReadBar {
    input: ReadBarInput
    output: ReadBarOutput
}

@input
structure ReadBarInput {
    barId: String
}

structure ReadBarOutput {}

@updatesResources([
    {
        resource: "com.example#Bar"
        identifiers: { barId: { path: "barId" } }
    }
])
operation UpdateBar {
    input: UpdateBarInput
    output: UpdateBarOutput
}

@input
structure UpdateBarInput {
    barId: String
}

structure UpdateBarOutput {}
