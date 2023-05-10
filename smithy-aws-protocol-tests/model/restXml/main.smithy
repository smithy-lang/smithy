$version: "2.0"

namespace aws.protocoltests.restxml

use aws.api#service
use aws.protocols#restXml
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// A REST XML service that sends XML requests and responses.
@service(sdkId: "Rest Xml Protocol")
@restXml
service RestXml {
    version: "2019-12-16",
    operations: [
        // Basic input and output tests
        NoInputAndNoOutput,
        NoInputAndOutput,
        EmptyInputAndEmptyOutput,
        XmlEmptyLists,
        XmlEmptyMaps,
        XmlEmptyStrings,

        // @httpHeader tests
        InputAndOutputWithHeaders,
        NullAndEmptyHeadersClient,
        NullAndEmptyHeadersServer,
        TimestampFormatHeaders,

        // @httpLabel tests
        HttpRequestWithLabels,
        HttpRequestWithLabelsAndTimestampFormat,
        HttpRequestWithGreedyLabelInPath,
        HttpRequestWithFloatLabels,

        // @httpQuery and @httpQueryParams tests
        AllQueryStringTypes,
        ConstantQueryString,
        ConstantAndVariableQueryString,
        IgnoreQueryParamsInResponse,
        OmitsNullSerializesEmptyString,
        QueryIdempotencyTokenAutoFill,
        QueryPrecedence,
        QueryParamsAsStringListMap,

        // @httpPrefixHeaders tests
        HttpPrefixHeaders,

        // @httpPayload tests
        HttpPayloadTraits,
        HttpPayloadTraitsWithMediaType,
        HttpPayloadWithStructure,
        HttpPayloadWithXmlName,
        BodyWithXmlName,
        HttpPayloadWithMemberXmlName,
        HttpPayloadWithXmlNamespace,
        HttpPayloadWithXmlNamespaceAndPrefix,

        // @httpResponseCode tests
        HttpResponseCode,

        // Output tests
        XmlEmptyBlobs,

        // Errors
        GreetingWithErrors,

        // Synthesized XML document body tests
        SimpleScalarProperties,
        XmlUnions,
        XmlBlobs,
        XmlTimestamps,
        XmlEnums,
        XmlIntEnums,
        RecursiveShapes,
        XmlLists,
        XmlMaps,
        XmlMapsXmlName,
        NestedXmlMaps,
        FlattenedXmlMap,
        FlattenedXmlMapWithXmlName,
        FlattenedXmlMapWithXmlNamespace,

        // @xmlAttribute tests
        XmlAttributes,
        XmlAttributesOnPayload,

        // @xmlNamespace trait tests
        XmlNamespaces,

        // @endpoint and @hostLabel trait tests
        EndpointOperation,
        EndpointWithHostLabelOperation,
        EndpointWithHostLabelHeaderOperation,

        // client-only timestamp parsing tests
        DatetimeOffsets,
        FractionalSeconds,

        // requestCompression trait tests
        PutWithContentEncoding
    ]
}
