$version: "2"

namespace smithy.benchmark.serde

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restXml
use smithy.rules#endpointRuleSet

@title("AWS REST XML Data Plane")
@sigv4(name: "example")
@restXml
@xmlNamespace(uri: "https://awsrestxmldataplane.amazonaws.com")
@service(sdkId: "RestXmlDataPlane")
@endpointRuleSet({
    version: "1.0"
    parameters: {
        Region: {
            builtIn: "AWS::Region"
            required: false
            documentation: "The AWS region used to dispatch the request. Unused."
            type: "String"
        }
        Endpoint: {
            builtIn: "SDK::Endpoint"
            required: false
            documentation: "Override the endpoint used to send this request. Unused."
            type: "String"
        }
    }
    rules: [
        {
            conditions: []
            endpoint: { url: "https://example.com" }
            type: "endpoint"
        }
    ]
})
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
