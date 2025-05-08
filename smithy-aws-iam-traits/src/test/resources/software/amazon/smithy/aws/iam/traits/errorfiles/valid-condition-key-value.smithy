$version: "2.0"

namespace smithy.example

use aws.api#service
use aws.iam#conditionKeyValue
use aws.iam#defineConditionKeys

@defineConditionKeys(
    "smithy:ActionContextKey1": { type: "String" }
    "myservice:ActionContextKey2": { type: "String" }
)
@service(sdkId: "My", arnNamespace: "myservice")
service MyService {
    version: "2019-02-20",
    operations: [Echo]
}

operation Echo {
    input: EchoInput
}

structure EchoInput {
    @conditionKeyValue("smithy:ActionContextKey1")
    id1: String

    @conditionKeyValue("ActionContextKey2")
    id2: String
}
