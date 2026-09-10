$version: "2"

namespace smithy.benchmark.serde

use aws.api#service
use aws.auth#sigv4
use smithy.protocols#rpcv2Cbor
use smithy.rules#endpointRuleSet

@title("Smithy RPC v2 CBOR Data Plane")
@sigv4(name: "example")
@rpcv2Cbor
@service(sdkId: "RpcCborDataPlane")
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
service SmithyRpcV2CborDataPlane {
    version: "1999-12-31"
    operations: [
        Healthcheck
    ]
    resources: [
        DynamoDBItem
        CloudWatchMetric
    ]
}
