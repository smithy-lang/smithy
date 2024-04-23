namespace smithy.example

use aws.cloudformation#cfnMutability
use aws.cloudformation#cfnResource

service TestService {
    version: "2020-07-02"
    resources: [
        PutResource
    ]
}

@cfnResource(
    additionalSchemas: [FooProperties]
)
resource PutResource {
    identifiers: { fooId: String }
    put: PutFoo
}

@idempotent
operation PutFoo {
    input: PutFooRequest
    output: PutFooResponse
}

structure PutFooRequest {
    @required
    fooId: String

    @cfnMutability("full")
    tags: TagList
}

structure PutFooResponse {
    fooId: String
}

structure FooProperties {
    @cfnMutability("full")
    barProperty: String
}

list TagList {
    member: String
}
