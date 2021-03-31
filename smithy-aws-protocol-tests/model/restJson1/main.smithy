$version: "1.0"

namespace aws.protocoltests.restjson

use aws.api#service
use aws.protocols#restJson1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// A REST JSON service that sends JSON requests and responses.
@service(sdkId: "Rest Json Protocol")
@restJson1
service RestJson {
    version: "2019-12-16",
    // Ensure that generators are able to handle renames.
    rename: {
        "aws.protocoltests.restjson.nested#GreetingStruct": "RenamedGreeting",
    },
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
        MediaTypeHeader,

        // @httpLabel tests
        HttpRequestWithLabels,
        HttpRequestWithLabelsAndTimestampFormat,
        HttpRequestWithGreedyLabelInPath,

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
        HttpPrefixHeadersResponse,

        // @httpPayload tests
        HttpPayloadTraits,
        HttpPayloadTraitsWithMediaType,
        HttpPayloadWithStructure,
        HttpEnumPayload,
        HttpStringPayload,

        // @httpResponseCode tests
        HttpResponseCode,

        // @streaming tests
        StreamingTraits,
        StreamingTraitsRequireLength,
        StreamingTraitsWithMediaType,

        // Errors
        GreetingWithErrors,

        // Synthesized JSON document body tests
        SimpleScalarProperties,
        JsonTimestamps,
        JsonEnums,
        RecursiveShapes,
        JsonLists,
        JsonMaps,
        JsonBlobs,

        // Documents
        InlineDocument,
        InlineDocumentAsPayload,

        // Unions
        JsonUnions,

        // @endpoint and @hostLabel trait tests
        EndpointOperation,
        EndpointWithHostLabelOperation,
    ]
}
