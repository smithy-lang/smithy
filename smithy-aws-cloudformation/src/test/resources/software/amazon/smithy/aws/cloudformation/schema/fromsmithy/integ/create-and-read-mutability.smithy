namespace smithy.example

use aws.cloudformation#cfnMutability
use aws.cloudformation#cfnResource

service TestService {
    version: "2020-07-02"
    resources: [
        CreateAndRead
    ]
}

@cfnResource(
    additionalSchemas: [FooProperties]
)
resource CreateAndRead {
    identifiers: { fooId: String }
}

structure FooProperties {
    @cfnMutability("create-and-read")
    immutableSetting: Boolean
}
