$version: "2"

namespace smithy.benchmark.serde

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restJson1
use smithy.rules#endpointRuleSet

@title("AWS REST JSON Data Plane")
@sigv4(name: "example")
@restJson1
@service(sdkId: "RestJsonDataPlane")
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
service AwsRestJsonDataPlane {
    version: "1999-12-31"
    operations: [
        Healthcheck
        GetObjectStreaming
    ]
    resources: [
        S3Object
        CloudWatchMetric
    ]
}
