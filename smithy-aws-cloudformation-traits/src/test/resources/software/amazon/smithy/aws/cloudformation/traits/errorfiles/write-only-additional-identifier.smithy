$version: "2.0"

namespace smithy.example

use aws.cloudformation#cfnAdditionalIdentifier
use aws.cloudformation#cfnMutability

structure FooStructure {
    @cfnAdditionalIdentifier
    @cfnMutability("write")
    member: String
}
