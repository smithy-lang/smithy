$version: "2.0"

namespace aws.protocoltests.restjson

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restJson1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// A REST JSON service that sends JSON requests and responses.
@service(sdkId: "Rest Json Protocol")
@sigv4(name: "restjson")
@restJson1
@title("Sample Rest Json Protocol Service")
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
        UnitInputAndOutput,

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
        HttpRequestWithFloatLabels,
        HttpRequestWithRegexLiteral,

        // @httpQuery and @httpQueryParams tests
        AllQueryStringTypes,
        ConstantQueryString,
        ConstantAndVariableQueryString,
        IgnoreQueryParamsInResponse,
        OmitsNullSerializesEmptyString,
        OmitsSerializingEmptyLists,
        QueryIdempotencyTokenAutoFill,
        QueryPrecedence,
        QueryParamsAsStringListMap,

        // @httpPrefixHeaders tests
        HttpPrefixHeaders,
        HttpPrefixHeadersInResponse,

        // @httpPayload tests
        HttpPayloadTraits,
        HttpPayloadTraitsWithMediaType,
        HttpPayloadWithStructure,
        HttpEnumPayload,
        HttpStringPayload,
        HttpPayloadWithUnion,

        // @httpResponseCode tests
        HttpResponseCode,
        ResponseCodeRequired
        ResponseCodeHttpFallback

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
        JsonIntEnums,
        RecursiveShapes,
        JsonLists,
        SparseJsonLists,
        JsonMaps,
        SparseJsonMaps,
        JsonBlobs,

        // Documents
        DocumentType,
        DocumentTypeAsPayload,
        DocumentTypeAsMapValue,

        // Unions
        JsonUnions,
        PostPlayerAction,
        PostUnionWithJsonName,

        // @endpoint and @hostLabel trait tests
        EndpointOperation,
        EndpointWithHostLabelOperation,

        // custom endpoints with paths
        HostWithPathOperation,

        // checksum(s)
        HttpChecksumRequired,

        // malformed request tests
        MalformedRequestBody,
        MalformedInteger,
        MalformedUnion,
        MalformedBoolean,
        MalformedList,
        MalformedMap,
        MalformedBlob,
        MalformedByte,
        MalformedShort,
        MalformedLong,
        MalformedFloat,
        MalformedDouble,
        MalformedString,
        MalformedTimestampPathDefault,
        MalformedTimestampPathHttpDate,
        MalformedTimestampPathEpoch,
        MalformedTimestampQueryDefault,
        MalformedTimestampQueryHttpDate,
        MalformedTimestampQueryEpoch,
        MalformedTimestampHeaderDefault,
        MalformedTimestampHeaderDateTime,
        MalformedTimestampHeaderEpoch,
        MalformedTimestampBodyDefault,
        MalformedTimestampBodyDateTime,
        MalformedTimestampBodyHttpDate,
        MalformedContentTypeWithoutBody,
        MalformedContentTypeWithoutBodyEmptyInput
        MalformedContentTypeWithBody,
        MalformedContentTypeWithPayload,
        MalformedContentTypeWithGenericString,
        MalformedAcceptWithBody,
        MalformedAcceptWithPayload,
        MalformedAcceptWithGenericString,

        // request body and content-type handling
        TestBodyStructure,
        TestPayloadStructure,
        TestPayloadBlob,
        TestGetNoPayload
        TestPostNoPayload,
        TestGetNoInputNoPayload,
        TestPostNoInputNoPayload,

        // client-only timestamp parsing tests
        DatetimeOffsets,
        FractionalSeconds,

        // requestCompression trait tests
        PutWithContentEncoding,

        // Content-Type header tests
        ContentTypeParameters,

        // defaults
        OperationWithDefaults
        OperationWithNestedStructure
    ]
}
