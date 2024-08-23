$version: "2.0"

namespace smithy.example

/// Documentation
@auth([httpBasicAuth])
service Foo {
    version: "2"
    operations: [
        GetTime1
    ]
    resources: []
    errors: [
        E1
        E2
    ]
}

operation GetTime1 {}

resource Sprocket1 {
    operations: [
        GetTime2
    ]
}

operation GetTime2 {
    input := {}
}

@http(method: "X", uri: "/foo", code: 200)
resource Sprocket2 {
    identifiers: {
        username: String
        id: String
        otherId: String
    }
    properties: {
        foo: String
        bar: Integer
        fizz: String
    }
}

@error("client")
structure E1 {}

@error("client")
structure E2 {}
