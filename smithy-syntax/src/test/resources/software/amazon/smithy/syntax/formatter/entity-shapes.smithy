$version: "2.0"

namespace smithy.example

/// Documentation
@auth([httpBasicAuth])
service Foo {
    version: "1"
    operations: [
        GetTime1
        GetTime2
    ]
    resources: [
        Sprocket1
        Sprocket2
    ]
    rename: {
        "smithy.example#SomeOperationFoo": "SomeOperationFooRenamed"
    }
}

service Foo2 {
    version: "2"
    operations: []
    resources: []
    rename: {}
}

operation GetTime1 {}

operation GetTime2 {
    input := {}
}

resource Sprocket1 {
    identifiers: {username: String}
}

@http(method: "X", uri: "/foo", code: 200)
resource Sprocket2 {
    identifiers: {username: String, id: String, otherId: String}
    collectionOperations: [
        SomeOperation
    ]
}

operation SomeOperation {
    input := {
        foo: SomeOperationFoo
    }
}

structure SomeOperationFoo {}

@http(method: "X", uri: "/foo3", code: 200)
resource Sprocket3 {
    identifiers: {username: String, id: String, otherId: String}
    // It's empty, so on a single line.
    collectionOperations: []
}
