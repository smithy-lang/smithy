namespace smithy.example

use aws.cloudformation#cfnResource

service TestService {
    version: "2020-07-02"
    resources: [
        CreateWrite
    ]
}

@cfnResource
resource CreateWrite {
    identifiers: { fooId: String }
    create: CreateFoo
}

operation CreateFoo {
    input: CreateFooRequest
    output: CreateFooResponse
}

structure CreateFooRequest {
    createWriteProperty: String
}

structure CreateFooResponse {
    fooId: String
}
