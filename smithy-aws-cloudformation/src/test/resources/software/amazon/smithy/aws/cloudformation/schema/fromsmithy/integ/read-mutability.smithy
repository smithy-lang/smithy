namespace smithy.example

use aws.cloudformation#cfnMutability
use aws.cloudformation#cfnResource

service TestService {
    version: "2020-07-02"
    resources: [
        Read
    ]
}

@cfnResource(
    additionalSchemas: [FooProperties]
)
resource Read {
    identifiers: { fooId: String }
    read: GetFoo
}

@readonly
operation GetFoo {
    input: GetFooRequest
    output: GetFooResponse
}

structure GetFooRequest {
    @required
    fooId: String
}

structure GetFooResponse {
    @cfnMutability("read")
    updatedAt: Timestamp
}

structure FooProperties {
    @cfnMutability("read")
    createdAt: Timestamp
}
