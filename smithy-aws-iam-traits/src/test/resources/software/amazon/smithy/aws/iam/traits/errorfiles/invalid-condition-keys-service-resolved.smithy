$version: "2.0"

namespace smithy.example

@aws.api#service(sdkId: "My")
@aws.iam#defineConditionKeys(
    "smithy:ActionContextKey1": { type: "String" }
)
@aws.iam#serviceResolvedConditionKeys(["smithy:invalidkey"])
service MyService {
    version: "2019-02-20",
    operations: [Echo]
}

operation Echo {}
