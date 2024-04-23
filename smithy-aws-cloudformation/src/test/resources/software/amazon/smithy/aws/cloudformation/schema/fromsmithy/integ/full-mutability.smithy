namespace smithy.example

use aws.cloudformation#cfnMutability
use aws.cloudformation#cfnResource

service TestService {
    version: "2020-07-02"
    resources: [
        Full
    ]
}

@cfnResource(
    additionalSchemas: [FooProperties]
)
resource Full {
    identifiers: { fooId: String }
    create: CreateFoo
}

operation CreateFoo {
    input: CreateFooRequest
    output: CreateFooResponse
}

structure CreateFooRequest {
    @cfnMutability("full")
    tags: TagList
}

structure CreateFooResponse {
    fooId: String
}

structure FooProperties {
    @cfnMutability("full")
    barProperty: String
}

list TagList {
    member: String
}
