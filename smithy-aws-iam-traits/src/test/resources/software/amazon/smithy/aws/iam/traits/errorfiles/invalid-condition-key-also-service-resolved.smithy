$version: "2.0"

namespace smithy.example

use aws.iam#conditionKeyValue

@aws.iam#defineConditionKeys(
    "smithy:ServiceResolveContextKey": { type: "String" }
)
@aws.iam#serviceResolvedConditionKeys(["smithy:ServiceResolveContextKey"])
@aws.api#service(sdkId: "My")
service MyService {
    version: "2019-02-20",
    operations: [Echo]
}

operation Echo {
    input: EchoInput
}

structure EchoInput {
    @aws.iam#conditionKeyValue("smithy:ServiceResolveContextKey")
    id1: String
}
