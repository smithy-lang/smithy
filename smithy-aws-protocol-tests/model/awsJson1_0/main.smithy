$version: "2.0"

namespace aws.protocoltests.json10

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsJson1_0
use aws.protocols#awsQueryCompatible
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@service(sdkId: "JSON RPC 10")
@sigv4(name: "jsonrpc10")
@awsJson1_0
@title("Sample Json 1.0 Protocol Service")
service JsonRpc10 {
    version: "2020-07-14",
    operations: [
        // Basic input and output tests
        NoInputAndNoOutput,
        NoInputAndOutput,
        EmptyInputAndEmptyOutput,
        SimpleScalarProperties,

        // Errors
        GreetingWithErrors,
        JsonUnions,

        // @endpoint and @hostLabel trait tests
        EndpointOperation,
        EndpointWithHostLabelOperation,

        // custom endpoints with paths
        HostWithPathOperation,

        // requestCompression trait tests
        PutWithContentEncoding,

        // Content-Type header tests
        ContentTypeParameters,

        OperationWithDefaults,
        OperationWithRequiredMembers,
        OperationWithNestedStructure
        OperationWithRequiredMembersWithDefaults

        QueryIncompatibleOperation
    ]
}


@service(sdkId: "Query Compatible JSON RPC 10")
@sigv4(name: "query-compatible-jsonrpc10")
@awsJson1_0
@title("Query Compatible Json 1.0 Protocol Service")
@awsQueryCompatible
service QueryCompatibleJsonRpc10 {
    version: "2020-07-14",
    operations: [
        QueryCompatibleOperation
    ]
}
