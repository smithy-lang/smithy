namespace smithy.example

use aws.cloudformation#cfnMutability
use aws.cloudformation#cfnResource

service TestService {
    version: "2020-07-02"
    resources: [
        Write
    ]
}

@cfnResource(
    additionalSchemas: [FooProperties]
)
resource Write {
    identifiers: { fooId: String }
    create: CreateFoo
}

operation CreateFoo {
    input: CreateFooRequest
    output: CreateFooResponse
}

structure CreateFooRequest {
    @cfnMutability("write")
    secret: String
}

structure CreateFooResponse {
    fooId: String
}

structure FooProperties {
    @cfnMutability("write")
    password: String
}
