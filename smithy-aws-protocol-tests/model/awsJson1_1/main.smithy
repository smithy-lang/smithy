$version: "1.0"

namespace aws.protocoltests.json

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsJson1_1

@service(
    sdkId: "Json Protocol",
    arnNamespace: "jsonprotocol",
    cloudFormationName: "JsonProtocol",
    cloudTrailEventSource: "jsonprotocol.amazonaws.com",
)
@sigv4(name: "foo",)
@awsJson1_1
@title("Sample Json 1.1 Protocol Service")
service JsonProtocol {
    version: "2018-01-01",
    operations: [
        EmptyOperation,
        KitchenSinkOperation,
        OperationWithOptionalInputOutput,
        PutAndGetDocuments
    ],
}

// Shared types

structure SimpleStruct {
    Value: smithy.api#String,
}
