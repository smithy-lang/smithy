$version: "2.0"

metadata suppressions = [
    {
        id: "XmlFlattenedTrait"
        namespace: "aws.protocoltests.query"
        reason: """
            Some tests are for asserting the correct behavior in the case that
            xmlFlattened is wrong and trips this validator."""
    }
]

namespace aws.protocoltests.query

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsQuery
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// A query service that sends query requests and XML responses.
@service(sdkId: "Query Protocol")
@sigv4(name: "awsquery")
@awsQuery
@xmlNamespace(uri: "https://example.com/")
@title("Sample Query Protocol Service")
service AwsQuery {
    version: "2020-01-08",
    operations: [
        // Basic input and output tests
        NoInputAndNoOutput,
        NoInputAndOutput,
        EmptyInputAndEmptyOutput,

        // Input tests
        SimpleInputParams,
        QueryTimestamps,
        NestedStructures,
        QueryLists,
        QueryMaps,
        QueryIdempotencyTokenAutoFill,

        // Output tests
        XmlEmptyBlobs,

        // Output XML map tests
        XmlMaps,
        XmlMapsXmlName,
        FlattenedXmlMap,
        FlattenedXmlMapWithXmlName,
        FlattenedXmlMapWithXmlNamespace,
        XmlEmptyMaps,

        // Output XML list tests
        XmlLists,
        XmlEmptyLists,

        // Output XML structure tests
        SimpleScalarXmlProperties,
        XmlBlobs,
        XmlTimestamps,
        XmlEnums,
        XmlIntEnums,
        RecursiveXmlShapes,
        RecursiveXmlShapes,
        IgnoresWrappingXmlName,
        XmlNamespaces,

        // Output error tests
        GreetingWithErrors,

        // @endpoint and @hostLabel trait tests
        EndpointOperation,
        EndpointWithHostLabelOperation,

        // custom endpoints with paths
        HostWithPathOperation,

        // client-only timestamp parsing tests
        DatetimeOffsets,
        FractionalSeconds,

        // requestCompression trait tests
        PutWithContentEncoding
    ]
}
