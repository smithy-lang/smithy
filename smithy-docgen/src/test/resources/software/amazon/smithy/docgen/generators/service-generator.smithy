$version: "2"

namespace smithy.example

@httpBearerAuth
service TestService {
    operations: [
        BasicOperation
    ]
}

operation BasicOperation {
    input := {
        foo: String
    }
    output := {
        bar: String
    }
}
