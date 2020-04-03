$version: "1.0.0"

namespace aws.protocoltests.query

use aws.api#service
use aws.protocols#awsQuery
use aws.api#service
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// A query service that sends query requests and XML responses.
@service(sdkId: "Query Protocol")
@awsQuery
@xmlNamespace(uri: "https://example.com/")
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

        // Output XML map tests
        XmlMaps,
        XmlMapsXmlName,
        FlattenedXmlMap,
        FlattenedXmlMapWithXmlName,

        // Output XML list tests
        XmlLists,

        // Output XML structure tests
        SimpleScalarXmlProperties,
        XmlBlobs,
        XmlTimestamps,
        XmlEnums,
        RecursiveXmlShapes,
        RecursiveXmlShapes,
        IgnoresWrappingXmlName,
        XmlNamespaces,

        // Output error tests
        GreetingWithErrors,
    ]
}
