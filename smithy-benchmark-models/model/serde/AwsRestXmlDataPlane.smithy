$version: "2"

namespace smithy.benchmark.serde

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restXml

@title("AWS REST XML Data Plane")
@sigv4(name: "awsrestxmldataplane")
@restXml
@xmlNamespace(uri: "https://awsrestxmldataplane.amazonaws.com")
@service(sdkId: "RestXmlDataPlane")
service AwsRestXmlDataPlane {
    version: "1999-12-31"
    operations: [
        Healthcheck
    ]
    resources: [
        S3Object
        CloudWatchMetric
    ]
}
