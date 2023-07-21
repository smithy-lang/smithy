$version: "2.0"

namespace smithy.example

/// Documentation
@auth([httpBasicAuth])
service Foo {
    version: "2"
    operations: [GetTime1, GetTime2]
    resources: [Sprocket1, Sprocket2]
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
}
