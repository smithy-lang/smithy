$version: "2"

namespace smithy.benchmark.serde

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsJson1_0

@title("AWS JSON RPC 1.0 Data Plane")
@sigv4(name: "awsjsonrpc10dataplane")
@awsJson1_0
@service(sdkId: "JsonRpc10DataPlane")
service AwsJsonRpc10DataPlane {
    version: "1999-12-31"
    operations: [
        Healthcheck
    ]
    resources: [
        DynamoDBItem
        CloudWatchMetric
    ]
}
