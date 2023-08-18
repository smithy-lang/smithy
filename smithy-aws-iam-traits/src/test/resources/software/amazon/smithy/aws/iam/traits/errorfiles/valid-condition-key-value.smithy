$version: "2.0"

namespace smithy.example

use aws.iam#conditionKeyValue

@aws.iam#defineConditionKeys(
    "smithy:ActionContextKey1": { type: "String" }
)
@aws.api#service(sdkId: "My")
service MyService {
    version: "2019-02-20",
    operations: [Echo]
}

operation Echo {
    input: EchoInput
}

structure EchoInput {
    @aws.iam#conditionKeyValue("smithy:ActionContextKey1")
    id1: String
}
