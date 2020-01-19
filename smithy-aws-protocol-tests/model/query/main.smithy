$version: "0.5.0"

namespace aws.protocols.tests.query

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// A query service that sends query requests and XML responses.
@protocols([{"name": "aws.query"}])
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

        // Output error tests
        GreetingWithErrors,
    ]
}
