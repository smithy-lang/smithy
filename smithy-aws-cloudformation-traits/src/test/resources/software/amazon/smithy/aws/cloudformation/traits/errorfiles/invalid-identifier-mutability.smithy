$version: "2.0"

namespace smithy.example

use aws.cloudformation#cfnMutability
use aws.cloudformation#cfnResource

@cfnResource(additionalSchemas: [FooOne, FooTwo, FooThree])
resource FooResource {
    identifiers: {
        member: String
    }
}

structure FooOne {
    @required
    @cfnMutability("full")
    member: String
}

structure FooTwo {
    @required
    @cfnMutability("write")
    member: String
}

structure FooThree {
    @required
    @cfnMutability("create")
    member: String
}