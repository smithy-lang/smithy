$version: "2.0"
namespace smithy.example

use aws.api#service
use aws.iam#defineConditionKeys
use aws.iam#serviceResolvedConditionKeys

@service(sdkId: "My", arnNamespace: "myservice")
@defineConditionKeys(
    "myservice:ServiceResolvedContextKey": { type: "String" }
    "myservice:AnotherResolvedContextKey": { type: "String" }
)
@serviceResolvedConditionKeys(["myservice:ServiceResolvedContextKey", "AnotherResolvedContextKey"])
service MyService {
    version: "2019-02-20",
}
