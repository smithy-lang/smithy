$version: "2.0"

namespace smithy.example

@aws.iam#defineConditionKeys(
    "smithy:ActionContextKey1": { type: "String" }
)
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
