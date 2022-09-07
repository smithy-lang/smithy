$version: "2.0"

namespace aws.protocoltests.json10

use aws.api#service
use aws.protocols#awsJson1_0
use aws.protocols#awsQueryCompatible
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests


@service(sdkId: "JSON RPC 10 with Query Compatible Trait")
@awsQueryCompatible
@awsJson1_0
service JsonRpc10WithQueryCompatible {
    version: "2020-07-14",
    operations: [GreetingWithErrors]
}
