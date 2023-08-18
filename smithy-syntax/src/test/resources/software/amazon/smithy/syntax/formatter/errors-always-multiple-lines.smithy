$version: "2.0"

namespace smithy.example

operation DeleteFoo1 {
    errors: [
        BadRequest
    ]
}

operation DeleteFoo2 {
    errors: [
        BadRequest
        WorseRequest
    ]
}

@error("client")
structure BadRequest {}

@error("client")
structure WorseRequest {}
