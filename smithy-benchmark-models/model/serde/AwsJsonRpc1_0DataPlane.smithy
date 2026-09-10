$version: "2"

namespace smithy.benchmark.serde

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsJson1_0
use smithy.rules#endpointRuleSet

@title("AWS JSON RPC 1.0 Data Plane")
@sigv4(name: "example")
@awsJson1_0
@service(sdkId: "JsonRpc10DataPlane")
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
