$version: "2.0"

namespace smithy.example

use aws.api#service
use aws.iam#conditionKeyValue
use aws.iam#defineConditionKeys

@defineConditionKeys(
    "myservice:ActionContextKey1": { type: "String" }
)
@service(sdkId: "My", arnNamespace: "myservice")
service MyService {
    version: "2019-02-20",
    operations: [Echo]
}

operation Echo {
    input := {
        @conditionKeyValue("myservice:InvalidConditionKey")
        id1: String

        @conditionKeyValue("myservice:ActionContextKey1")
        id2: String

        @conditionKeyValue("myservice:ActionContextKey1")
        id3: String
    }
}
