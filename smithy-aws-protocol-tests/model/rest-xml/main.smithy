$version: "0.5.0"

namespace aws.protocols.tests.restxml

use aws.api#service
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// A REST XML service that sends XML requests and responses.
@service(sdkId: "Rest Xml Protocol")
@protocols([{"name": "aws.rest-xml"}])
service RestXml {
    version: "2019-12-16",
    operations: [
        // Basic input and output tests
        NoInputAndNoOutput,
        NoInputAndOutput,
        EmptyInputAndEmptyOutput,

        // @httpHeader tests
        InputAndOutputWithHeaders,
        NullAndEmptyHeadersClient,
        NullAndEmptyHeadersServer,
        TimestampFormatHeaders,

        // @httpLabel tests
        HttpRequestWithLabels,
        HttpRequestWithLabelsAndTimestampFormat,
        HttpRequestWithGreedyLabelInPath,

        // @httpQuery tests
        AllQueryStringTypes,
        ConstantQueryString,
        ConstantAndVariableQueryString,
        IgnoreQueryParamsInResponse,
        OmitsNullSerializesEmptyString,
        QueryIdempotencyTokenAutoFill,

        // @httpPrefixHeaders tests
        HttpPrefixHeaders,

        // @httpPayload tests
        HttpPayloadTraits,
        HttpPayloadTraitsWithMediaType,
        HttpPayloadWithStructure,
        HttpPayloadWithXmlName,
        HttpPayloadWithXmlNamespace,
        HttpPayloadWithXmlNamespaceAndPrefix,

        // Errors
        GreetingWithErrors,

        // Synthesized XML document body tests
        SimpleScalarProperties,
        XmlBlobs,
        XmlTimestamps,
        XmlEnums,
        RecursiveShapes,
        XmlLists,
        XmlMaps,
        XmlMapsXmlName,
        FlattenedXmlMap,
        FlattenedXmlMapWithXmlName,

        // @xmlAttribute tests
        XmlAttributes,
        XmlAttributesOnPayload,

        // @xmlNamespace trait tests
        XmlNamespaces,
    ]
}
